package aeza.hostmaster.models;

import aeza.hostmaster.dto.MetricDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long metricId;
    private int agentId;
    private String agentName;
    private MetricType metricType;
    private double value;
    private LocalDateTime timestamp;

    public enum MetricType {
        AGENT_AVAILABILITY,
        REQUEST_COUNT,
        RESPONSE_COUNT
    }

    public MetricDTO toDTO() {
        MetricDTO dto = new MetricDTO();
        dto.setAgentId(agentId);
        dto.setAgentName(agentName);
        dto.setMetricType(metricType);
        dto.setValue(value);
        dto.setTimestamp(timestamp);
        return dto;
    }

    @Data
    public class AgentRating {
        private String agentId;
        private String agentName;
        private double averageLatency;
        private double averageAvailability;
        private int totalRequestCount;
    }
}
