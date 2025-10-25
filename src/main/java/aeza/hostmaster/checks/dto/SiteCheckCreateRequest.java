package aeza.hostmaster.checks.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SiteCheckCreateRequest(
        @NotNull
        @Size(min = 1, max = 2048)
        String target
) {}