package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.entity.SiteCheckEntity;
import aeza.hostmaster.checks.service.CheckJobService;
import aeza.hostmaster.checks.service.KafkaSiteCheckService;
import aeza.hostmaster.checks.service.SiteCheckStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checks")
public class SiteCheckController {

    private final CheckJobService jobService;
    private final KafkaSiteCheckService kafkaService;
    private final SiteCheckStorageService storageService;

    public SiteCheckController(CheckJobService jobService,
                               KafkaSiteCheckService kafkaService, SiteCheckStorageService storageService) {
        this.jobService = jobService;
        this.kafkaService = kafkaService;
        this.storageService = storageService;
    }

    // ------------------- CREATE CHECK -----------------------

    @PostMapping
    @Operation(summary = "Schedule a site check job")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Accepted"),
    })
    public ResponseEntity<CheckJobResponse> createSiteCheck(
            @RequestBody SiteCheckCreateRequest request
    ) {
        SiteCheckEntity job = jobService.createJob(request.target());

        // отправляем задачи агентам → Kafka
        kafkaService.dispatchTasks(job, request);

        CheckJobResponse resp = new CheckJobResponse(
                job.getId(),
                job.getTarget(),
                job.getStatus(),
                job.getExecutedAt(),
                job.getFinishedAt(),
                job.getTotalDurationMillis(),
                null
        );

        return ResponseEntity.accepted().body(resp);
    }


    // ------------------- GET JOB STATUS -----------------------
    @GetMapping("/{jobId}")
    @Operation(summary = "Fetch job status",
            description = "Returns last known job state")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404")
    })
    public ResponseEntity<CheckJobResponse> getJobStatus(@PathVariable UUID jobId) {

        var job = jobService.findJob(jobId);
        // findJob у тебя и так кидает 404 через SiteCheckNotFoundException, но ладно.

        var checks = storageService.buildExecutionDtosForJob(jobId);

        CheckJobResponse resp = new CheckJobResponse(
                job.getId(),
                job.getTarget(),
                job.getStatus(),
                job.getExecutedAt(),
                job.getFinishedAt(),
                job.getTotalDurationMillis(),
                checks          // <-- сюда кладём список execution’ов
        );

        return ResponseEntity.ok(resp);
    }
}
