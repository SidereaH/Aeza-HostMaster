package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SiteCheckResponse(
        UUID id,
        String target,
        Instant executedAt,
        CheckStatus status,
        Long totalDurationMillis,
        List<CheckExecutionResponse> checks
) {
}
