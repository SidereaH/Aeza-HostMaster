package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import java.time.Instant;
import java.util.UUID;

public record WebSocketMessage(
        String type, // "JOB_CREATED", "JOB_UPDATED", "JOB_COMPLETED"
        UUID jobId,
        CheckStatus status,
        Instant timestamp,
        Object data
) {}