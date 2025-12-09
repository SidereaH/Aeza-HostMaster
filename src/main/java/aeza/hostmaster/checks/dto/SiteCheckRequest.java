package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record SiteCheckRequest(
        String target,
        Instant executedAt,
        CheckStatus status,
        Long totalDurationMillis,
        List<CheckExecutionRequest> checks
) {}
