package aeza.hostmaster.metrics.dto;

import aeza.hostmaster.metrics.models.Metric;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class MetricDTO {
    private Long agentId;
    private String agentName;
    private Metric.MetricType metricType;
    private double value;
    private Instant timestamp;

    public Metric toMetric(){
        Metric m = new Metric();
        m.setAgentName(agentName);
        m.setMetricType(metricType);
        m.setValue(value);
//        m.setTimestamp(timestamp);
        return m;
    }
}
