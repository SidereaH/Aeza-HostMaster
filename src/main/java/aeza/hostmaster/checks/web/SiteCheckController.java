package aeza.hostmaster.checks.web;

import aeza.hostmaster.checks.dto.SiteCheckRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.service.SiteCheckService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/check-results")
public class SiteCheckController {

    private final SiteCheckService service;

    public SiteCheckController(SiteCheckService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiteCheckResponse create(@RequestBody SiteCheckRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public SiteCheckResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<SiteCheckResponse> find(
            @RequestParam(value = "target", required = false) String target,
            Pageable pageable) {
        return service.find(target, pageable);
    }
}
