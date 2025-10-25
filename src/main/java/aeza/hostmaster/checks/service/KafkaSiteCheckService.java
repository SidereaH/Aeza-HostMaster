package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaSiteCheckService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String SITE_CHECK_REQUESTS_TOPIC = "site-check-requests";

    public KafkaSiteCheckService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendSiteCheckTask(SiteCheckCreateRequest request) {
        try {
            UUID taskId = UUID.randomUUID();

            // Создаем задание для агента
            SiteCheckTask task = new SiteCheckTask(taskId, request.target(), "site-check-results");
            String taskJson = objectMapper.writeValueAsString(task);

            // Отправляем задание в Kafka (асинхронно)
            kafkaTemplate.send(SITE_CHECK_REQUESTS_TOPIC, taskId.toString(), taskJson);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send site check task to Kafka", e);
        }
    }
}