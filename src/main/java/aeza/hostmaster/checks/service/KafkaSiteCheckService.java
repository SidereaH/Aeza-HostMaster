package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.dto.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaSiteCheckService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SiteCheckStorageService storageService;
    private final CheckJobService jobService;

    private static final String SITE_CHECK_REQUESTS_TOPIC = "checks-requests";
    private static final String SITE_CHECK_RESULTS_TOPIC = "checks-results";

    public KafkaSiteCheckService(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 SiteCheckStorageService storageService,
                                 CheckJobService jobService) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.jobService = jobService;


    }
    public CheckJobResponse getJobStatus(UUID jobId) {
        return jobService.getJobStatus(jobId);
    }

    public CheckJobResponse createSiteCheckJob(String target) {
        // Создаем job в БД
        CheckJobResponse job = jobService.createJob(target);

        // Отправляем задание в Kafka
        SiteCheckTask task = new SiteCheckTask(job.jobId(), target, SITE_CHECK_RESULTS_TOPIC);

        try {
            String taskJson = objectMapper.writeValueAsString(task);
            kafkaTemplate.send(SITE_CHECK_REQUESTS_TOPIC, job.jobId().toString(), taskJson);

            // Обновляем статус на IN_PROGRESS
            jobService.updateJobStatus(job.jobId(), CheckStatus.IN_PROGRESS);

        } catch (Exception e) {
            jobService.updateJobStatus(job.jobId(), CheckStatus.FAILED);
            throw new RuntimeException("Failed to send site check task to Kafka", e);
        }

        return job;
    }

    @KafkaListener(topics = SITE_CHECK_RESULTS_TOPIC)
    public void handleSiteCheckResult(ConsumerRecord<String, String> record) {
        try {
            SiteCheckResult result = objectMapper.readValue(record.value(), SiteCheckResult.class);

            // Сохраняем результат в БД
            storageService.saveSiteCheck(result.response());

            // Обновляем статус job и отправляем WebSocket
            jobService.completeJob(result.taskId(), result.response());

        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse site check result: " + e.getMessage());
            // Обновляем статус на FAILED
            jobService.updateJobStatus(UUID.fromString(record.key()), CheckStatus.FAILED);
        }
    }
}