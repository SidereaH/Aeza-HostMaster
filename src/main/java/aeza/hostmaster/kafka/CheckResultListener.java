package aeza.hostmaster.kafka;

import java.util.UUID;

import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.service.CheckResultStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes check results from Kafka and keeps them in memory for quick retrieval via HTTP.
 */
@Component
@ConditionalOnProperty(value = "app.kafka.agent-listeners-enabled", havingValue = "true", matchIfMissing = true)
public class CheckResultListener {

    private static final Logger log = LoggerFactory.getLogger(CheckResultListener.class);

    private final CheckResultStore store;
    private final ObjectMapper objectMapper;

    public CheckResultListener(CheckResultStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.results-topic:checks-results}",
            groupId = "${app.kafka.result-cache-group:hostmaster-results-cache}"
    )
    public void onResult(ConsumerRecord<String, String> record) {
        String key = record.key();

        if (key == null) {
            log.warn("Received Kafka result without checkId key, skipping: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        try {
            UUID checkId = UUID.fromString(key);
            SiteCheckResponse response = deserialize(record.value());
            if (response != null) {
                store.store(checkId, response);
                log.info("Stored result for check {} from Kafka topic {}", checkId, record.topic());
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Received Kafka result with non-UUID key '{}', skipping", key);
        }
    }

    private SiteCheckResponse deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, SiteCheckResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize Kafka result payload: {}", ex.getOriginalMessage());
            return null;
        }
    }
}
