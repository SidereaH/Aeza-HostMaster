package aeza.hostmaster.checks.dto;

import java.util.UUID;

public record SiteCheckTask(
        UUID taskId,
        String target,
        String callbackTopic
) {}