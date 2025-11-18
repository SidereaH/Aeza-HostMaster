package aeza.hostmaster.kafka;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import aeza.hostmaster.service.CheckResultStore;

/**
 * Consumes check results from Kafka and keeps them in memory for quick retrieval via HTTP.
 */
@Component
@ConditionalOnProperty(value = "app.kafka.agent-listeners-enabled", havingValue = "true", matchIfMissing = true)
public class CheckResultListener {

    private static final Logger log = LoggerFactory.getLogger(CheckResultListener.class);

    private final CheckResultStore store;

    public CheckResultListener(CheckResultStore store) {
        this.store = store;
    }

    @KafkaListener(topics = "${app.kafka.results-topic:checks-results}")
    public void onResult(ConsumerRecord<String, String> record) {
        String key = record.key();

        if (key == null) {
            log.warn("Received Kafka result without checkId key, skipping: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        try {
            UUID checkId = UUID.fromString(key);
            store.store(checkId, record.value());
            log.info("Stored result for check {} from Kafka topic {}", checkId, record.topic());
        } catch (IllegalArgumentException ex) {
            log.warn("Received Kafka result with non-UUID key '{}', skipping", key);
        }
    }
}
