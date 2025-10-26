package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.AgentCheckResult;
import aeza.hostmaster.checks.dto.AgentTaskMessage;
import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckResult;
import aeza.hostmaster.checks.web.CheckResultsWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaSiteCheckService {

    private static final Logger log = LoggerFactory.getLogger(KafkaSiteCheckService.class);

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
            throw new RuntimeException("Failed to send site check task to Kafka", e);
        }

        return job;
    }

    public CheckJobResponse getJobStatus(UUID jobId) {
        return jobService.getJobStatus(jobId);
    }

    @KafkaListener(
            topics = CHECK_RESULTS_TOPIC,
            autoStartup = "${app.kafka.agent-listeners-enabled:true}"
    )
    public void handleSiteCheckResult(ConsumerRecord<String, String> record) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(record.value());
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse check result message for key {}: {}", record.key(), ex.getOriginalMessage());
            handleInvalidMessage(record.key());
            return;
        }

        UUID jobId = resolveJobId(record.key(), payload);
        if (jobId != null) {
            Object payloadForClient = extractPayloadForClient(payload);
            checkResultsWebSocketHandler.sendResult(jobId, payloadForClient);
        } else {
            log.debug("Unable to resolve job id for check result message with key {}", record.key());
        }

        if (tryProcessAggregatedResult(payload)) {
            return;
        }

        if (payload.isObject() && payload.hasNonNull("status")) {
            try {
                AgentCheckResult agentResult = objectMapper.treeToValue(payload, AgentCheckResult.class);
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
        UUID jobId;
        try {
            jobId = UUID.fromString(record.key());
        } catch (IllegalArgumentException ex) {
            log.warn("Received log with invalid job id key: {}", record.key());
            return;
        }

        Object payload;
        try {
            JsonNode jsonNode = objectMapper.readTree(record.value());
            payload = objectMapper.convertValue(jsonNode, Map.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse agent log for job {}: {}", jobId, ex.getMessage());
            payload = Map.of("message", record.value());
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
        try {
            var result = kafkaTemplate.send(topic, jobId.toString(), payload).get(5, TimeUnit.SECONDS);
            var md = result.getRecordMetadata();
            log.info("Sent to topic={} partition={} offset={} key={}", md.topic(), md.partition(), md.offset(), jobId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending task message to Kafka", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new RuntimeException("Failed to deliver task message to Kafka", ex);
        }
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

            storageService.saveSiteCheck(result.response());
            jobService.completeJob(result.taskId(), result.response());
            checkResultsWebSocketHandler.completeJob(result.taskId());
            return true;
        } catch (JsonProcessingException ex) {
            log.debug("Message is not an aggregated site check result: {}", ex.getOriginalMessage());
            return false;
        }
    }

    private UUID resolveJobId(String key, JsonNode payload) {
        if (key != null && !key.isBlank()) {
            try {
                return UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                // try to extract from payload
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
                for (String candidate : List.of("http", "ping", "dns", "tcp", "traceroute")) {
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

}
