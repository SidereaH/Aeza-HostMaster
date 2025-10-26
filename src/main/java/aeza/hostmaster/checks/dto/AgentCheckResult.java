package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record AgentCheckResult(
        @JsonProperty("task_id") String taskId,
        @JsonProperty("agent_id") String agentId,
        String status,
        Long duration,
        String error,
        String timestamp,
        JsonNode payload
) {
}
