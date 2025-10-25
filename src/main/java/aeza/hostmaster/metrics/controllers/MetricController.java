package aeza.hostmaster.metrics.controllers;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import aeza.hostmaster.metrics.dto.MetricDTO;
import aeza.hostmaster.metrics.services.MetricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class MetricController {
    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/metric")
    public ResponseEntity<?> receivedMetrics(@RequestBody MetricDTO metricDTO) {
        int available = 1;
        metricService.saveMetric(metricDTO);
        metricService.agentCounter(metricDTO.getAgentId());
        metricService.addAvailabilityCheck(available, metricDTO.getAgentId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metricDTO);
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getAgentMetrics(@PathVariable Long agentId) {
//        metricService.findAgent(agentDTO);
        metricService.averageLatency(agentId);
        AgentRatingDTO agentRatingDTO = metricService.findAgent(agentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(agentRatingDTO);
    }

}
