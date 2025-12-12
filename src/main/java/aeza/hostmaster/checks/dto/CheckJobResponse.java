package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import java.time.Instant;
import java.util.UUID;

public record CheckJobResponse(
        UUID id,
        String target,
        CheckStatus status,
        Instant executedAt,
        Instant finishedAt,
        Long totalDurationMillis,
        Object result
) {}
