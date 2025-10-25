package aeza.hostmaster.dto;

import aeza.hostmaster.models.Metric;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MetricDTO {
    private int agentId;
    private String agentName;
    private Metric.MetricType metricType;
    private double value;
    private LocalDateTime timestamp;

    public Metric toMetric(){
        Metric m = new Metric();
        m.setAgentId(agentId);
        m.setAgentName(agentName);
        m.setMetricType(metricType);
        m.setValue(value);
        m.setTimestamp(timestamp);
        return m;
    }
}
