package aeza.hostmaster.kafka;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.service.CheckResultStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
            topics = "${app.kafka.results-topic:check-results}",
            groupId = "${app.kafka.result-cache-group:hostmaster-results-cache}"
    )
    public void onResult(ConsumerRecord<String, String> record) {
        String key = record.key();

        UUID checkId = parseCheckIdFromKey(key);

        SiteCheckResponse response = deserialize(record.value(), checkId);
        if (response == null) {
            return;
        }

        UUID resolvedId = response.id();
        if (resolvedId == null) {
            log.warn("Skipping Kafka result because check id is missing; topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        store.store(resolvedId, response);
        log.info("Stored result for check {} from Kafka topic {}", resolvedId, record.topic());
    }

    private UUID parseCheckIdFromKey(String key) {
        if (key == null) {
            return null;
        }

        try {
            UUID checkId = UUID.fromString(key);
            SiteCheckResponse response = deserialize(record.value(), checkId);
            if (response != null) {
                store.store(checkId, response);
                log.info("Stored result for check {} from Kafka topic {}", checkId, record.topic());
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Received Kafka result with non-UUID key '{}', will try to read id from payload", key);
            return null;
        }
    }

    private SiteCheckResponse deserialize(String payload, UUID checkId) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!(root instanceof ObjectNode objectNode)) {
                log.warn("Kafka result payload is not an object; skipping cache save");
                return null;
            }

            SiteCheckResponse mapped = objectMapper.treeToValue(root, SiteCheckResponse.class);

            Instant executedAt = mapped != null ? mapped.executedAt() : null;
            if (executedAt == null) {
                executedAt = parseInstant(objectNode.get("timestamp"));
            }
            if (executedAt == null) {
                executedAt = parseInstant(objectNode.get("executed_at"));
            }
            if (executedAt == null) {
                executedAt = Instant.now();
            }

            CheckStatus status = mapped != null ? mapped.status() : null;
            if (status == null) {
                status = mapStatus(objectNode.path("status").asText());
            }
            if (status == null) {
                status = CheckStatus.COMPLETED;
            }

            Long duration = mapped != null ? mapped.totalDurationMillis() : null;
            if (duration == null) {
                JsonNode durationNode = objectNode.get("duration");
                if (durationNode != null && durationNode.canConvertToLong()) {
                    duration = durationNode.asLong();
                }
            }

            List<CheckExecutionResponse> checks = mapped != null ? mapped.checks() : null;
            if (checks == null || checks.isEmpty()) {
                checks = buildChecksFromPayload(objectNode);
            }

            return new SiteCheckResponse(
                    mapped != null && mapped.id() != null ? mapped.id() : checkId,
                    mapped != null ? mapped.target() : null,
                    executedAt,
                    status,
                    duration,
                    checks
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to deserialize Kafka result payload: {}", ex.getOriginalMessage());
            return null;
        }
    }

    private List<CheckExecutionResponse> buildChecksFromPayload(ObjectNode root) {
        ObjectNode payloadNode = root;
        JsonNode nestedPayload = root.get("payload");
        if (nestedPayload instanceof ObjectNode nestedObject) {
            payloadNode = nestedObject;
        }

        List<CheckExecutionResponse> checks = new ArrayList<>();

        JsonNode pingNode = root.get("ping");
        if (pingNode == null) {
            pingNode = payloadNode.get("ping");
        }

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

        return checks;
    }

    private PingCheckDetailsDto mapPingDetails(JsonNode pingNode) {
        JsonNode sourceNode = pingNode != null && pingNode.isArray() && pingNode.size() > 0 ? pingNode.get(0) : pingNode;
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
}
