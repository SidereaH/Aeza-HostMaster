package aeza.hostmaster.dto;

import lombok.Data;

@Data
public class AgentDTO {
    private Long agentId;
    private String agentName;
    private double averageLatency;
    private double averageAvailability;
    private int totalRequestCount;
}
