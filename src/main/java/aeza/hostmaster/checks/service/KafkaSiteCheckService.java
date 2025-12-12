package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.AgentTaskMessage;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.entity.CheckExecutionEntity;
import aeza.hostmaster.checks.entity.CheckLogEntity;
import aeza.hostmaster.checks.entity.SiteCheckEntity;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class KafkaSiteCheckService {

    private static final Logger log = LoggerFactory.getLogger(KafkaSiteCheckService.class);
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafka;

    private final ObjectMapper mapper;
    private final PayloadNormalizer normalizer;
    private final SiteCheckStorageService storageService;
    private final CheckJobService jobService;
    private final SimpMessagingTemplate ws;

    public KafkaSiteCheckService(org.springframework.kafka.core.KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                                 PayloadNormalizer normalizer,
                                 SiteCheckStorageService storageService,
                                 CheckJobService jobService,
                                 SimpMessagingTemplate ws) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.normalizer = normalizer;
        this.storageService = storageService;
        this.jobService = jobService;
        this.ws = ws;
    }


    // ----------------------- CHECK RESULTS --------------------------

    @KafkaListener(topics = "check-results", groupId = "hostmaster-group")
    public void handleCheckResult(ConsumerRecord<String, String> record) {
        if (record.value() == null || record.key() == null) {
            log.debug("Skip system log event");
            return;
        }
        JsonNode root;
        try {
            root = mapper.readTree(record.value());
        } catch (Exception e) {
            log.error("Invalid JSON: {}", record.value(), e);
            return;
        }

        String taskId = root.path("task_id").asText(null);
        if (taskId == null) {
            taskId = root.path("taskId").asText(null);
        }

        if (taskId == null) {
            log.error("❌ Missing task_id in Kafka message: {}", record.value());
            return;
        }

        UUID jobId;
        try {
            jobId = UUID.fromString(taskId);
        } catch (Exception e) {
            log.error("❌ Invalid task_id format: {}", taskId);
            return;
        }

        SiteCheckEntity job = storageService.getJob(jobId);
        if (job == null) {
            log.error("Job {} not found", jobId);
            return;
        }

        // 1 - partial execution
        CheckExecutionEntity exec = storageService.saveExecution(job, root);

        // 2 - update job status по статусу агента/агрегатора
        updateJobStatus(job, root);

        // 3 - normalize payload → DTO для фронта
        CheckExecutionResponse dto = storageService.buildExecutionDto(exec, root);

        // 4 - send to websocket
        log.info("WS DTO = {}", dto);
        ws.convertAndSend("/topic/jobs/" + jobId, dto);
    }



    // --------------------------- LOGS -------------------------------

    @KafkaListener(topics = "agent-logs", groupId = "hostmaster-group")
    public void handleAgentLog(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.debug("Skip system log event");
            return;
        }

        JsonNode root;
        try {
            root = mapper.readTree(record.value());
        } catch (JsonProcessingException e) {
            log.warn("Invalid log JSON: {}", record.value(), e);
            return;
        }

        String taskId = root.path("task_id").asText(null);
        if (taskId == null) {
            taskId = root.path("taskId").asText(null);
        }

        if (taskId == null) {
            log.warn("Log without task_id: {}", record.value());
            return;
        }

        UUID jobId;
        try {
            jobId = UUID.fromString(taskId);
        } catch (Exception e) {
            log.warn("Log with invalid task_id: {}", taskId);
            return;
        }

        SiteCheckEntity job = storageService.getJob(jobId);
        if (job == null) {
            log.warn("Job {} not found (log)", jobId);
            return;
        }

        CheckLogEntity logEntity = storageService.saveLog(job, record.value());
        log.info("WS DTO (log) = {}", logEntity);

        ws.convertAndSend("/topic/jobs/" + jobId, logEntity);
    }





    // ------------------------- HELPERS ------------------------------

    private UUID extractJobId(JsonNode root) {
        if (root.has("task_id")) {
            try { return UUID.fromString(root.get("task_id").asText()); }
            catch (Exception ignored) {}
        }
        if (root.has("taskId")) {
            try { return UUID.fromString(root.get("taskId").asText()); }
            catch (Exception ignored) {}
        }
        return null;
    }


    private void updateJobStatus(SiteCheckEntity job, JsonNode root) {

        String status = root.path("status").asText("").toLowerCase();

        // статус от агента
        switch (status) {
            case "failed", "error" -> {
                jobService.updateStatus(job, CheckStatus.FAILED);
                return;
            }
            case "success", "completed" -> {
                // для одиночных чеков можно сразу считать job выполненной,
                // но если есть агрегатор, он пришлёт "response" и мы завершили job там.
                // Сейчас оставим только IN_PROGRESS, а финал — по aggregated response.
                jobService.updateStatus(job, CheckStatus.COMPLETED);
            }
            default -> {
                // неизвестный статус – не трогаем job
            }
        }

        // aggregated result от какого-то агрегатора (старый формат)
        if (root.has("response") && !root.get("response").isNull()) {
            try {
                SiteCheckResponse resp = mapper.treeToValue(root.get("response"), SiteCheckResponse.class);
                jobService.completeJob(job, resp);
            } catch (Exception e) {
                log.error("Failed to parse aggregated response", e);
            }
        }
    }

    public void dispatchTasks(SiteCheckEntity job, SiteCheckCreateRequest req) {

        if (req.checkTypes() == null || req.checkTypes().isEmpty()) {
            throw new IllegalArgumentException("checkTypes cannot be empty");
        }

        for (CheckType type : req.checkTypes()) {

            try {
                var json = mapper.writeValueAsString(
                        new AgentTaskMessage(
                                job.getId().toString(),
                                type.name().toLowerCase(),
                                req.target(),
                                req.parameters(),
                                job.getExecutedAt(),
                                Instant.now(),
                                req.timeoutSeconds()
                        )
                );

                kafka.send("agent-tasks", job.getId().toString(), json);
                log.info("TASK SENT to agent-tasks: job={} type={}", job.getId(), type);

            } catch (Exception e) {
                log.error("Failed to send task {} for job {}", type, job.getId(), e);
                jobService.updateStatus(job, CheckStatus.FAILED);
            }
        }

        jobService.updateStatus(job, CheckStatus.IN_PROGRESS);
    }



}
