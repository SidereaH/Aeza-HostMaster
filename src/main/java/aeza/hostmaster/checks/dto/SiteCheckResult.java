package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SiteCheckResult(
        @JsonProperty("taskId")
        @JsonAlias("task_id")
        UUID taskId,
        @JsonAlias({"response", "result", "data", "payload"})
        SiteCheckResponse response
) {}
