package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SiteCheckResponse(
        @JsonAlias({"id", "task_id", "taskId"})
        UUID id,
        @JsonAlias({"target", "hostname", "host"})
        String target,
        @JsonAlias({"executed_at", "started_at", "startedAt", "timestamp"})
        Instant executedAt,
        @JsonAlias({"status", "result"})
        CheckStatus status,
        @JsonAlias({"total_duration_millis", "total_duration_ms", "totalDuration", "duration", "duration_ms"})
        Long totalDurationMillis,
        @JsonAlias({"checks", "results", "details"})
        List<CheckExecutionResponse> checks
) {
}
