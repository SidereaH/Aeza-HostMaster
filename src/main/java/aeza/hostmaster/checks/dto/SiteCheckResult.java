package aeza.hostmaster.checks.dto;

import java.util.UUID;

public record SiteCheckResult(
        UUID taskId,
        SiteCheckResponse response
) {}