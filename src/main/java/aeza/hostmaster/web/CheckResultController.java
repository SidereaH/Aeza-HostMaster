package aeza.hostmaster.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import aeza.hostmaster.service.CheckResultStore;

@RestController
@RequestMapping("/api/checks")
public class CheckResultController {

    private final CheckResultStore store;

    public CheckResultController(CheckResultStore store) {
        this.store = store;
    }

    @GetMapping("/{checkId}/result")
    public ResponseEntity<CheckResultResponse> getResult(@PathVariable UUID checkId) {
        return store.find(checkId)
                .map(payload -> ResponseEntity.ok(new CheckResultResponse(checkId, "COMPLETED", payload)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(new CheckResultResponse(checkId, "PENDING", null)));
    }

    public record CheckResultResponse(UUID checkId, String status, String payload) {
    }
}
