package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.service.KafkaSiteCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-checks")
public class SiteCheckController {

    private final KafkaSiteCheckService kafkaSiteCheckService;

    public SiteCheckController(KafkaSiteCheckService kafkaSiteCheckService) {
        this.kafkaSiteCheckService = kafkaSiteCheckService;
    }

    @PostMapping
    public ResponseEntity<String> createSiteCheck(
            @RequestBody SiteCheckCreateRequest request) {

        try {
            kafkaSiteCheckService.sendSiteCheckTask(request);
            return ResponseEntity.accepted().body("Site check task sent to Kafka");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send task to Kafka: " + e.getMessage());
        }
    }
}