package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.service.KafkaSiteCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checks")
public class SiteCheckController {

    private final KafkaSiteCheckService kafkaSiteCheckService;

    public SiteCheckController(KafkaSiteCheckService kafkaSiteCheckService) {
        this.kafkaSiteCheckService = kafkaSiteCheckService;
    }

    @PostMapping
    public ResponseEntity<CheckJobResponse> createSiteCheck(
            @RequestBody SiteCheckCreateRequest request) {

        CheckJobResponse job = kafkaSiteCheckService.createSiteCheckJob(request); // Передаем весь request
        return ResponseEntity.accepted().body(job);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<CheckJobResponse> getCheckStatus(@PathVariable UUID jobId) {
        try {
            CheckJobResponse job = kafkaSiteCheckService.getJobStatus(jobId);
            return ResponseEntity.ok(job);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}