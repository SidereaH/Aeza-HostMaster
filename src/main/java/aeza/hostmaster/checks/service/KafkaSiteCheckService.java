package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.AgentCheckResult;
import aeza.hostmaster.checks.dto.AgentTaskMessage;
import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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

    private static final String AGENT_TASKS_TOPIC = "agent-tasks";
    private static final String CHECK_RESULTS_TOPIC = "check-results";
    private static final String LEGACY_CHECK_RESULTS_TOPIC = "checks-results";
    private static final String AGENT_LOGS_TOPIC = "agent-logs";

    public KafkaSiteCheckService(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 SiteCheckStorageService storageService,
                                 CheckJobService jobService) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.jobService = jobService;
    }

    public CheckJobResponse createSiteCheckJob(SiteCheckCreateRequest request) {
        CheckJobResponse job = jobService.createJob(request.target());

        List<CheckType> checkTypes = request.checkTypes() == null || request.checkTypes().isEmpty()
                ? List.of(CheckType.HTTP)
                : request.checkTypes();

        Instant createdAt = job.executedAt() != null ? job.executedAt() : Instant.now();
        Instant scheduledAt = createdAt;

        try {
            for (CheckType checkType : checkTypes) {
                AgentTaskMessage taskMessage = buildAgentTaskMessage(job, request, checkType, scheduledAt, createdAt);
                String payload = objectMapper.writeValueAsString(taskMessage);
                kafkaTemplate.send(AGENT_TASKS_TOPIC, job.jobId().toString(), payload);
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

    @KafkaListener(topics = CHECK_RESULTS_TOPIC)
    public void handleAgentCheckResult(ConsumerRecord<String, String> record) {
        try {
            AgentCheckResult agentResult = objectMapper.readValue(record.value(), AgentCheckResult.class);
            processAgentCheckResult(agentResult);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse agent check result for key {}: {}", record.key(), e.getMessage());
            try {
                jobService.updateJobStatus(UUID.fromString(record.key()), CheckStatus.FAILED);
            } catch (IllegalArgumentException ex) {
                log.error("Unable to update job status for malformed key {}", record.key());
            }
        }
    }

    @KafkaListener(topics = LEGACY_CHECK_RESULTS_TOPIC)
    public void handleAggregatedSiteCheckResult(ConsumerRecord<String, String> record) {
        try {
            SiteCheckResult result = objectMapper.readValue(record.value(), SiteCheckResult.class);
            if (result.response() == null) {
                log.debug("Aggregated result missing payload for job {}", result.taskId());
                return;
            }

            storageService.saveSiteCheck(result.response());
            jobService.completeJob(result.taskId(), result.response());
        } catch (JsonProcessingException ex) {
            log.debug("Failed to parse aggregated site check result for key {}: {}", record.key(), ex.getOriginalMessage());
        }
    }

    @KafkaListener(topics = AGENT_LOGS_TOPIC)
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
                                                  Instant scheduledAt,
                                                  Instant createdAt) {
        Map<String, Object> parameters = buildParameters(checkType, request);

        return new AgentTaskMessage(
                job.jobId().toString(),
                normalizeType(checkType),
                request.target(),
                parameters,
                scheduledAt,
                createdAt,
                resolveTimeout(checkType, request)
        );
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
