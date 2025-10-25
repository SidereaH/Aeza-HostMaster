package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.SiteCheckCreateRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.service.SiteCheckService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-checks")
public class SiteCheckController {

    private final SiteCheckService siteCheckService;

    public SiteCheckController(SiteCheckService siteCheckService) {
        this.siteCheckService = siteCheckService;
    }

    @PostMapping
    public ResponseEntity<SiteCheckResponse> createSiteCheck(
            @Valid @RequestBody SiteCheckCreateRequest request) {

        SiteCheckResponse response = siteCheckService.performSiteCheck(request);
        return ResponseEntity.ok(response);
    }
}