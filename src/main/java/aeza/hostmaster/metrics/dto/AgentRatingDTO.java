package aeza.hostmaster.metrics.dto;

import lombok.Data;

@Data
public class AgentRatingDTO {
    private String agentName;
    private double averageLatency;
    private double averageAvailability;
    private int totalRequestCount;
}
