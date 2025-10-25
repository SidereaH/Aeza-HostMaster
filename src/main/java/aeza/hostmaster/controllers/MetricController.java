package aeza.hostmaster.controllers;

import aeza.hostmaster.models.Metric;
import aeza.hostmaster.services.MetricService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
