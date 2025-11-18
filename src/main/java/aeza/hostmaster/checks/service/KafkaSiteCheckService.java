package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.AgentCheckResult;
import aeza.hostmaster.checks.dto.AgentTaskMessage;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.dto.SiteCheckResult;
import aeza.hostmaster.checks.web.CheckResultsWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class KafkaSiteCheckService {

    private static final Logger log = LoggerFactory.getLogger(KafkaSiteCheckService.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SiteCheckStorageService storageService;
    private final CheckJobService jobService;
    private final CheckResultsWebSocketHandler checkResultsWebSocketHandler;

    private static final String AGENT_TASKS_TOPIC = "agent-tasks";
    private static final String AGENT_BROADCAST_TASKS_TOPIC = "agent-tasks-ping";
    private static final String CHECK_RESULTS_TOPIC = "check-results";
    private static final String AGENT_LOGS_TOPIC = "agent-logs";

    public KafkaSiteCheckService(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 SiteCheckStorageService storageService,
                                 CheckJobService jobService,
                                 CheckResultsWebSocketHandler checkResultsWebSocketHandler) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.jobService = jobService;
        this.checkResultsWebSocketHandler = checkResultsWebSocketHandler;
    }

    public CheckJobResponse createSiteCheckJob(SiteCheckCreateRequest request) {
        CheckJobResponse job = jobService.createJob(request.target());

        List<CheckType> checkTypes = request.checkTypes() == null || request.checkTypes().isEmpty()
                ? List.of(CheckType.HTTP)
                : request.checkTypes();

        Instant now = Instant.now();

        try {
            for (CheckType checkType : checkTypes) {
                AgentTaskMessage taskMessage = buildAgentTaskMessage(job, request, checkType, now);
                String payload = objectMapper.writeValueAsString(taskMessage);
                sendTaskMessage(checkType, job.jobId(), payload);
            }

            jobService.updateJobStatus(job.jobId(), CheckStatus.IN_PROGRESS);
        } catch (Exception e) {
            jobService.updateJobStatus(job.jobId(), CheckStatus.FAILED);
            throw new SiteCheckSchedulingException("Failed to send site check task to Kafka", e);
        }

        return job;
    }

    public CheckJobResponse getJobStatus(UUID jobId) {
        return jobService.getJobStatus(jobId);
    }

    @KafkaListener(
            topics = CHECK_RESULTS_TOPIC,
            autoStartup = "${app.kafka.agent-listeners-enabled:false}"
    )
    public void handleSiteCheckResult(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.debug("Received null check result for key {}", record.key());
            return;
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(record.value());
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse check result message for key {}: {}", record.key(), ex.getOriginalMessage());
            handleInvalidMessage(record.key());
            return;
        }

        JsonNode normalizedPayload = normalizeIncomingPayload(payload);

        UUID jobId = resolveJobId(record.key(), normalizedPayload);
        if (jobId != null) {
            Object payloadForClient = extractPayloadForClient(normalizedPayload);
            checkResultsWebSocketHandler.sendResult(jobId, payloadForClient);
            if (tryPersistAgentPayload(normalizedPayload, jobId)) {
                return;
            }
        } else {
            log.debug("Unable to resolve job id for check result message with key {}", record.key());
        }

        if (tryProcessAggregatedResult(normalizedPayload)) {
            return;
        }

        if (normalizedPayload.isObject() && normalizedPayload.hasNonNull("status")) {
            try {
                AgentCheckResult agentResult = objectMapper.treeToValue(normalizedPayload, AgentCheckResult.class);
                processAgentCheckResult(agentResult);
            } catch (JsonProcessingException ex) {
                log.warn("Failed to map agent check result for key {}: {}", record.key(), ex.getOriginalMessage());
            }
        }
    }

    @KafkaListener(
            topics = AGENT_LOGS_TOPIC,
            autoStartup = "${app.kafka.agent-listeners-enabled:false}"
    )
    public void handleAgentLog(ConsumerRecord<String, String> record) {
        JsonNode jsonPayload = null;
        Map<String, Object> payload = Map.of("message", record.value());

        try {
            jsonPayload = objectMapper.readTree(record.value());
            try {
                payload = objectMapper.convertValue(jsonPayload, Map.class);
            } catch (IllegalArgumentException ex) {
                log.warn("Failed to map agent log payload for key {}: {}", record.key(), ex.getMessage());
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse agent log payload for key {}: {}", record.key(), ex.getOriginalMessage());
        }

        UUID jobId = resolveJobId(record.key(), jsonPayload);
        if (jobId == null) {
            log.warn("Received log with invalid job id key: {}", record.key());
            return;
        }

        jobService.appendJobLog(jobId, payload);
    }

    private AgentTaskMessage buildAgentTaskMessage(CheckJobResponse job,
                                                  SiteCheckCreateRequest request,
                                                  CheckType checkType,
                                                  Instant scheduledAt) {
        Map<String, Object> parameters = buildParameters(checkType, request);

        return new AgentTaskMessage(
                job.jobId().toString(),
                normalizeType(checkType),
                request.target(),
                parameters,
                scheduledAt,
                Instant.now(),
                resolveTimeout(checkType, request)
        );
    }

    private void sendTaskMessage(CheckType checkType, UUID jobId, String payload) {
        String topic = resolveTopic(checkType);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, jobId.toString(), payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to deliver task message to Kafka for job {}: {}", jobId, ex.getMessage());
                jobService.updateJobStatus(jobId, CheckStatus.FAILED);
                checkResultsWebSocketHandler.completeJob(jobId);
                return;
            }

            if (result != null) {
                RecordMetadata md = result.getRecordMetadata();
                log.info("Sent to topic={} partition={} offset={} key={}", md.topic(), md.partition(), md.offset(), jobId);
            }
        });
    }


    private String resolveTopic(CheckType checkType) {
//        if (checkType == CheckType.PING) {
//            return AGENT_BROADCAST_TASKS_TOPIC;
//        }

        return AGENT_TASKS_TOPIC;
    }

    private Map<String, Object> buildParameters(CheckType checkType, SiteCheckCreateRequest request) {
        Map<String, Object> params = new HashMap<>();

        switch (checkType) {
            case HTTP -> {
                params.put("method", "GET");
                params.put("timeout", 5);
            }
            case TCP, TCP_CONNECT -> {
                SiteCheckCreateRequest.TcpCheckConfig tcp = request.tcpConfig();
                if (tcp != null) {
                    if (tcp.port() != null) {
                        params.put("port", tcp.port());
                    }
                    if (tcp.timeoutMillis() != null) {
                        params.put("timeout_millis", tcp.timeoutMillis());
                    }
                }
            }
            case DNS_LOOKUP -> {
                SiteCheckCreateRequest.DnsLookupConfig dns = request.dnsConfig();
                if (dns != null) {
                    if (dns.recordTypes() != null && !dns.recordTypes().isEmpty()) {
                        params.put("record_types", dns.recordTypes().stream()
                                .map(Enum::name)
                                .collect(Collectors.toList()));
                    }
                    if (dns.dnsServer() != null) {
                        params.put("dns_server", dns.dnsServer());
                    }
                }
            }
            case TRACEROUTE -> {
                SiteCheckCreateRequest.TracerouteConfig traceroute = request.tracerouteConfig();
                if (traceroute != null) {
                    if (traceroute.maxHops() != null) {
                        params.put("max_hops", traceroute.maxHops());
                    }
                    if (traceroute.timeoutMillis() != null) {
                        params.put("timeout_millis", traceroute.timeoutMillis());
                    }
                }
            }
            case PING -> {
                params.put("count", 4);
            }
        }

        return params;
    }

    private String normalizeType(CheckType type) {
        return switch (type) {
            case HTTP -> "http";
            case PING -> "ping";
            case TCP, TCP_CONNECT -> "tcp";
            case DNS_LOOKUP -> "dns";
            case TRACEROUTE -> "traceroute";
        };
    }

    private int resolveTimeout(CheckType type, SiteCheckCreateRequest request) {
        return switch (type) {
            case HTTP, PING -> 10;
            case TCP, TCP_CONNECT -> request.tcpConfig() != null && request.tcpConfig().timeoutMillis() != null
                    ? Math.toIntExact(Math.max(1, request.tcpConfig().timeoutMillis() / 1000))
                    : 10;
            case DNS_LOOKUP -> 10;
            case TRACEROUTE -> request.tracerouteConfig() != null && request.tracerouteConfig().timeoutMillis() != null
                    ? Math.toIntExact(Math.max(1, request.tracerouteConfig().timeoutMillis() / 1000))
                    : 30;
        };
    }

    private boolean tryProcessAggregatedResult(JsonNode payload) {
        try {
            SiteCheckResult result = objectMapper.treeToValue(payload, SiteCheckResult.class);
            if (result.taskId() == null || result.response() == null) {
                log.debug("Aggregated result missing required fields");
                return false;
            }

            SiteCheckResponse response = normalizeAggregatedResponse(result);

            storageService.saveSiteCheck(response);
            jobService.completeJob(result.taskId(), response);
            checkResultsWebSocketHandler.completeJob(result.taskId());
            return true;
        } catch (JsonProcessingException ex) {
            log.debug("Message is not an aggregated site check result: {}", ex.getOriginalMessage());
            return false;
        }
    }

    private boolean tryPersistAgentPayload(JsonNode payload, UUID jobId) {
        JsonNode responseNode = payload.get("response");
        if (!(responseNode instanceof ObjectNode response)) {
            return false;
        }

        SiteCheckResponse mappedResponse = mapResponse(jobId, payload, response);
        if (mappedResponse != null) {
            storageService.saveSiteCheck(mappedResponse);

            if (CheckStatus.COMPLETED.equals(mappedResponse.status())) {
                jobService.completeJob(jobId, mappedResponse);
                checkResultsWebSocketHandler.completeJob(jobId);
            } else if (CheckStatus.FAILED.equals(mappedResponse.status())) {
                jobService.updateJobStatus(jobId, CheckStatus.FAILED);
            } else if (mappedResponse.status() != null) {
                jobService.updateJobStatus(jobId, mappedResponse.status());
            }

            return true;
        }

        List<CheckExecutionResponse> checks = buildChecksFromAgentPayload(response);
        if (checks.isEmpty()) {
            return false;
        }

        Instant executedAt = parseInstant(payload.get("executed_at"));
        if (executedAt == null) {
            executedAt = parseInstant(payload.get("timestamp"));
        }
        if (executedAt == null) {
            executedAt = Instant.now();
        }

        Long totalDuration = null;
        JsonNode durationNode = payload.get("duration");
        if (durationNode != null && durationNode.canConvertToLong()) {
            totalDuration = durationNode.asLong();
        }

        CheckStatus status = mapStatus(payload.path("status").asText());
        if (status == null) {
            status = CheckStatus.COMPLETED;
        }

        SiteCheckResponse responseDto = new SiteCheckResponse(
                jobId,
                null,
                executedAt,
                status,
                totalDuration,
                checks
        );

        storageService.saveSiteCheck(responseDto);
        if (CheckStatus.COMPLETED.equals(status)) {
            jobService.completeJob(jobId, responseDto);
            checkResultsWebSocketHandler.completeJob(jobId);
        } else {
            jobService.updateJobStatus(jobId, status);
        }
        return true;
    }

    private SiteCheckResponse mapResponse(UUID jobId, JsonNode payload, ObjectNode response) {
        try {
            SiteCheckResponse responseDto = objectMapper.treeToValue(response, SiteCheckResponse.class);
            if (responseDto == null) {
                return null;
            }

            Instant executedAt = responseDto.executedAt();
            if (executedAt == null) {
                executedAt = parseInstant(payload.get("executed_at"));
            }
            if (executedAt == null) {
                executedAt = parseInstant(payload.get("timestamp"));
            }
            if (executedAt == null) {
                executedAt = Instant.now();
            }

            CheckStatus status = responseDto.status();
            if (status == null) {
                status = mapStatus(payload.path("status").asText());
            }
            if (status == null) {
                status = CheckStatus.COMPLETED;
            }

            return new SiteCheckResponse(
                    responseDto.id() != null ? responseDto.id() : jobId,
                    responseDto.target(),
                    executedAt,
                    status,
                    responseDto.totalDurationMillis(),
                    responseDto.checks()
            );
        } catch (JsonProcessingException ex) {
            log.debug("Failed to map agent payload to SiteCheckResponse for job {}: {}", jobId, ex.getOriginalMessage());
            return null;
        }
    }

    private List<CheckExecutionResponse> buildChecksFromAgentPayload(ObjectNode response) {
        List<CheckExecutionResponse> checks = new ArrayList<>();

        JsonNode pingNode = response.get("ping");
        if (pingNode != null) {
            PingCheckDetailsDto pingDetails = mapPingDetails(pingNode);
            if (pingDetails != null) {
                checks.add(new CheckExecutionResponse(
                        UUID.randomUUID(),
                        CheckType.PING,
                        CheckStatus.OK,
                        null,
                        null,
                        null,
                        pingDetails,
                        null,
                        null,
                        null,
                        List.of()
                ));
            }
        }

        return checks;
    }

    private PingCheckDetailsDto mapPingDetails(JsonNode pingNode) {
        JsonNode sourceNode = pingNode.isArray() && pingNode.size() > 0 ? pingNode.get(0) : pingNode;
        if (!(sourceNode instanceof ObjectNode source)) {
            return null;
        }

        JsonNode packets = source.get("packets");
        JsonNode roundTrip = source.get("roundTrip");

        Integer sent = packets != null && packets.hasNonNull("transmitted")
                ? packets.get("transmitted").asInt()
                : null;
        Integer received = packets != null && packets.hasNonNull("received")
                ? packets.get("received").asInt()
                : null;
        Double loss = packets != null ? parsePercentage(packets.get("loss")) : null;

        Double min = roundTrip != null ? parseMillis(roundTrip.get("min")) : null;
        Double avg = roundTrip != null ? parseMillis(roundTrip.get("avg")) : null;
        Double max = roundTrip != null ? parseMillis(roundTrip.get("max")) : null;

        return new PingCheckDetailsDto(sent, received, loss, min, avg, max, null);
    }

    private Double parsePercentage(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        try {
            return Double.parseDouble(text.replace("%", ""));
        } catch (NumberFormatException ex) {
            log.debug("Unable to parse percentage from {}", text);
            return null;
        }
    }

    private Double parseMillis(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }

        String text = value.asText();
        text = text.replace("ms", "").trim();
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            log.debug("Unable to parse millis from {}", text);
            return null;
        }
    }

    private Instant parseInstant(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }

        try {
            return Instant.parse(value.asText());
        } catch (Exception ex) {
            log.debug("Unable to parse instant from {}", value.asText());
            return null;
        }
    }

    private CheckStatus mapStatus(String statusText) {
        return CheckStatus.fromJson(statusText);
    }

    private SiteCheckResponse normalizeAggregatedResponse(SiteCheckResult result) {
        SiteCheckResponse raw = result.response();

        UUID responseId = raw.id() != null ? raw.id() : result.taskId();
        Instant executedAt = raw.executedAt() != null ? raw.executedAt() : Instant.now();
        CheckStatus status = raw.status() != null ? raw.status() : CheckStatus.COMPLETED;
        Long totalDuration = raw.totalDurationMillis();

        List<CheckExecutionResponse> normalizedChecks = (raw.checks() == null
                ? Stream.<CheckExecutionResponse>empty()
                : raw.checks().stream())
                .map(this::normalizeCheckExecution)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        return new SiteCheckResponse(
                responseId,
                raw.target(),
                executedAt,
                status,
                totalDuration,
                normalizedChecks
        );
    }

    private CheckExecutionResponse normalizeCheckExecution(CheckExecutionResponse check) {
        if (check == null) {
            return null;
        }

        CheckStatus status = check.status() != null ? check.status() : CheckStatus.OK;
        List<CheckMetricDto> metrics = check.metrics() == null ? List.of() : check.metrics();

        return new CheckExecutionResponse(
                check.id(),
                check.type(),
                status,
                check.durationMillis(),
                check.message(),
                check.httpDetails(),
                check.pingDetails(),
                check.tcpDetails(),
                check.tracerouteDetails(),
                check.dnsLookupDetails(),
                metrics
        );
    }

    private UUID resolveJobId(String key, JsonNode payload) {
        if (key != null && !key.isBlank()) {
            try {
                return UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                Matcher matcher = UUID_PATTERN.matcher(key);
                if (matcher.find()) {
                    try {
                        return UUID.fromString(matcher.group(1));
                    } catch (IllegalArgumentException ignoredExtract) {
                        log.debug("Failed to extract UUID from key {}", key);
                    }
                }
            }
        }

        if (payload != null && payload.isObject()) {
            JsonNode idNode = payload.has("taskId") ? payload.get("taskId") : payload.get("task_id");
            if (idNode != null && !idNode.isNull()) {
                try {
                    return UUID.fromString(idNode.asText());
                } catch (IllegalArgumentException ignored) {
                    log.debug("Payload contains invalid task id: {}", idNode.asText());
                }
            }
        }

        return null;
    }

    private Object extractPayloadForClient(JsonNode payload) {
        if (payload == null) {
            return null;
        }

        JsonNode data = null;
        String type = null;

        if (payload.isObject()) {
            JsonNode typeNode = payload.get("type");
            if (typeNode != null && !typeNode.isNull()) {
                type = typeNode.asText();
            }

            JsonNode responseNode = payload.get("response");
            if (responseNode != null && !responseNode.isNull()) {
                data = responseNode;
            }

            if (data == null) {
                for (String candidate : List.of("payload", "http", "ping", "dns", "tcp", "traceroute")) {
                    JsonNode candidateNode = payload.get(candidate);
                    if (candidateNode != null && !candidateNode.isNull()) {
                        data = candidateNode;
                        if (type == null) {
                            type = candidate;
                        }
                        break;
                    }
                }
            }

            if (data == null) {
                JsonNode resultNode = payload.get("result");
                if (resultNode != null && !resultNode.isNull()) {
                    data = resultNode;
                }
            }

            if (type == null) {
                JsonNode checkType = payload.get("check_type");
                if (checkType != null && !checkType.isNull()) {
                    type = checkType.asText();
                }
            }
        }

        if (data == null) {
            data = payload;
        }

        if (type == null) {
            return data;
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", type);
        result.set("data", data);
        return result;
    }

    private void handleInvalidMessage(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            jobService.updateJobStatus(UUID.fromString(key), CheckStatus.FAILED);
        } catch (IllegalArgumentException ex) {
            log.error("Unable to update job status for malformed key {}", key);
        }
    }

    private void processAgentCheckResult(AgentCheckResult result) {
        UUID jobId;
        try {
            jobId = UUID.fromString(result.taskId());
        } catch (IllegalArgumentException ex) {
            log.warn("Received agent result with invalid job id: {}", result.taskId());
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("agent_id", result.agentId());
        payload.put("status", result.status());
        payload.put("duration", result.duration());
        payload.put("error", result.error());
        payload.put("timestamp", result.timestamp());

        jobService.appendJobLog(jobId, payload);

        if (result.status() != null) {
            if ("success".equalsIgnoreCase(result.status())) {
                jobService.updateJobStatus(jobId, CheckStatus.COMPLETED);
            } else if ("failed".equalsIgnoreCase(result.status()) || "error".equalsIgnoreCase(result.status())) {
                jobService.updateJobStatus(jobId, CheckStatus.FAILED);
            }
        }
    }

    private JsonNode normalizeIncomingPayload(JsonNode payload) {
        if (!(payload instanceof ObjectNode objectNode)) {
            return payload;
        }

        ObjectNode normalized = objectNode.deepCopy();

        if (!normalized.hasNonNull("response")) {
            for (String candidate : List.of("payload", "data", "result")) {
                JsonNode candidateNode = normalized.get(candidate);
                if (candidateNode != null && !candidateNode.isNull()) {
                    normalized.set("response", candidateNode);
                    break;
                }
            }
        }

        JsonNode responseNode = normalized.get("response");
        if (responseNode instanceof ObjectNode responseObject) {
            copyIfAbsent(responseObject, "status", normalized, List.of("status"));
            copyIfAbsent(responseObject, "executed_at", normalized, List.of("executed_at", "timestamp", "finished_at", "completed_at"));
            copyIfAbsent(responseObject, "target", normalized, List.of("target", "hostname", "host"));

            if (!responseObject.hasNonNull("checks")) {
                for (String candidate : List.of("checks", "results", "details")) {
                    JsonNode candidateNode = responseObject.get(candidate);
                    if (candidateNode instanceof ArrayNode arrayNode) {
                        responseObject.set("checks", arrayNode);
                        break;
                    }
                }
            }

            JsonNode checksNode = responseObject.get("checks");
            if (checksNode instanceof ArrayNode checksArray) {
                for (JsonNode checkNode : checksArray) {
                    if (!(checkNode instanceof ObjectNode checkObject)) {
                        continue;
                    }

                    promoteFromSelf(checkObject, "type", List.of("type", "check_type", "name"));
                    promoteFromSelf(checkObject, "status", List.of("status", "state", "result_status"));
                    promoteFromSelf(checkObject, "durationMillis", List.of("durationMillis", "duration", "duration_ms", "elapsed_ms"));

                    JsonNode detailNode = firstPresent(checkObject, List.of("result", "details", "payload"));
                    if (detailNode != null && !detailNode.isNull()) {
                        String detailField = resolveDetailField(checkObject.path("type").asText(null));
                        if (detailField != null && !checkObject.hasNonNull(detailField)) {
                            checkObject.set(detailField, detailNode);
                        }
                    }
                }
            }
        }

        return normalized;
    }

    private void copyIfAbsent(ObjectNode target, String canonicalField, ObjectNode source, List<String> candidates) {
        if (target.hasNonNull(canonicalField)) {
            return;
        }

        for (String candidate : candidates) {
            JsonNode candidateNode = source.get(candidate);
            if (candidateNode != null && !candidateNode.isNull()) {
                target.set(canonicalField, candidateNode);
                return;
            }
        }
    }

    private void promoteFromSelf(ObjectNode target, String canonicalField, List<String> candidates) {
        if (target.hasNonNull(canonicalField)) {
            return;
        }

        for (String candidate : candidates) {
            JsonNode candidateNode = target.get(candidate);
            if (candidateNode != null && !candidateNode.isNull()) {
                target.set(canonicalField, candidateNode);
                return;
            }
        }
    }

    private JsonNode firstPresent(ObjectNode target, List<String> fields) {
        for (String field : fields) {
            JsonNode node = target.get(field);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private String resolveDetailField(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        CheckType type = CheckType.fromJson(rawType);
        if (type == null) {
            return null;
        }

        return switch (type) {
            case HTTP -> "http";
            case PING -> "ping";
            case TCP, TCP_CONNECT -> "tcp";
            case DNS_LOOKUP -> "dns";
            case TRACEROUTE -> "traceroute";
        };
    }
}
