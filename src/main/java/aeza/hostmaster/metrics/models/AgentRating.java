package aeza.hostmaster.metrics.models;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class AgentRating {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long agentRatingId;
    private int agentId; //id самого агента
    private String agentName;
    private double averageLatency;
    private double averageAvailability;
    private int totalRequestCount;

    public AgentRatingDTO toDTO() {
        AgentRatingDTO agentRatingDTO = new AgentRatingDTO();
        agentRatingDTO.setAgentName(this.agentName);
        agentRatingDTO.setAverageLatency(this.averageLatency);
        agentRatingDTO.setAverageAvailability(this.averageAvailability);
        agentRatingDTO.setTotalRequestCount(this.totalRequestCount);
        return agentRatingDTO;
    }
}
