package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.service.KafkaSiteCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/checks")
@Tag(name = "Site Checks", description = "Operations for scheduling and monitoring site availability checks")
public class SiteCheckController {

    private final KafkaSiteCheckService kafkaSiteCheckService;

    public SiteCheckController(KafkaSiteCheckService kafkaSiteCheckService) {
        this.kafkaSiteCheckService = kafkaSiteCheckService;
    }

    @PostMapping
    @Operation(
            summary = "Schedule a site check",
            description = "Creates a new asynchronous site check job and returns its identifier.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Check job accepted for processing",
                    content = @Content(schema = @Schema(implementation = CheckJobResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation failed for the supplied site parameters"),
            @ApiResponse(responseCode = "401", description = "Authentication is required to access this endpoint")
    })
    public ResponseEntity<CheckJobResponse> createSiteCheck(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Parameters describing the site check to be scheduled",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SiteCheckCreateRequest.class))
            )
            @RequestBody SiteCheckCreateRequest request) {

        CheckJobResponse job = kafkaSiteCheckService.createSiteCheckJob(request);
        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/{jobId}")
    @Operation(
            summary = "Fetch the status of a site check",
            description = "Returns the latest known status and metadata for the given job identifier.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CheckJobResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Job with the specified identifier was not found"),
            @ApiResponse(responseCode = "401", description = "Authentication is required to access this endpoint")
    })
    public ResponseEntity<CheckJobResponse> getCheckStatus(
            @Parameter(
                    description = "Identifier of the job whose status should be returned",
                    required = true,
                    schema = @Schema(implementation = UUID.class, format = "uuid"),
                    example = "6f46b7c4-74f4-4388-8f77-5fb547e1f3c9"
            )
            @PathVariable UUID jobId) {
        CheckJobResponse job = kafkaSiteCheckService.getJobStatus(jobId);
        return ResponseEntity.ok(job);
    }
}