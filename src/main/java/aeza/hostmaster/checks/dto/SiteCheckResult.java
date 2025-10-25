package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record SiteCheckResult(
        @JsonProperty("taskId")
        @JsonAlias("task_id")
        UUID taskId,
        SiteCheckResponse response
) {}
