package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record AgentTaskMessage(
        String id,
        String type,
        String target,
        Map<String, Object> parameters,
        @JsonProperty("scheduled_at") Instant scheduledAt,
        @JsonProperty("created_at") Instant createdAt,
        Integer timeout
) {
}
