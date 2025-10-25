package aeza.hostmaster.controllers;

import aeza.hostmaster.dto.AgentDTO;
import aeza.hostmaster.models.AgentRating;
import aeza.hostmaster.models.Metric;
import aeza.hostmaster.services.MetricService;
import lombok.Data;
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
    public ResponseEntity<?> receivedMetrics(@RequestBody Metric metric) {
        metricService.saveMetric(metric.toDTO());
        return ResponseEntity.ok("Metric received: " + metric);
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getAgentMetrics(@PathVariable Long agentId, @RequestBody AgentDTO agentDTO) {
//        metricService.findAgent(agentDTO);
        metricService.findAgent(agentId);
        return ResponseEntity.ok("Agent: " + agentDTO);
    }
}
