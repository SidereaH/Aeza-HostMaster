package aeza.hostmaster.metrics.controllers;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import aeza.hostmaster.metrics.dto.MetricDTO;
import aeza.hostmaster.metrics.services.MetricService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MetricController {
    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/metric")
    public ResponseEntity<?> receivedMetrics(@RequestBody MetricDTO metricDTO) {
        MetricDTO dto = metricService.saveMetric(metricDTO);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metricDTO);
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getAgentMetrics(@PathVariable Long agentId) {
//        metricService.findAgent(agentDTO);
        AgentRatingDTO agentRatingDTO = metricService.findAgent(agentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(agentRatingDTO);
    }
}
