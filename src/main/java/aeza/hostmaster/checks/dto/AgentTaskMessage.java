package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public record AgentTaskMessage(
        @JsonProperty("task_id")
        String taskId,

        @JsonProperty("type")
        String type,          // "http", "ping", "dns", "tcp", "traceroute"

        @JsonProperty("target")
        String target,

        @JsonProperty("parameters")
        Map<String, Object> parameters,

        @JsonProperty("scheduled_at")
        Instant scheduledAt,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("timeout")
        int timeoutSeconds
) {}
