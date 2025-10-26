package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Status information about a site check job")
public record CheckJobResponse(
        @Schema(description = "Unique identifier of the job", example = "6f46b7c4-74f4-4388-8f77-5fb547e1f3c9")
        UUID jobId,

        @Schema(description = "Target associated with the site check", example = "https://status.example.com")
        String target,

        @Schema(description = "Current execution state of the job")
        CheckStatus status,

        @Schema(description = "Timestamp when execution started", example = "2023-09-19T12:45:21Z")
        Instant executedAt,

        @Schema(description = "Timestamp when execution finished", example = "2023-09-19T12:45:37Z")
        Instant finishedAt,

        @Schema(description = "Total time spent processing the job in milliseconds", example = "16000")
        Long totalDurationMillis,

        @Schema(description = "Detailed result of the site check, if available")
        SiteCheckResponse result
) {}