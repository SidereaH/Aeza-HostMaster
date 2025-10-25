package aeza.hostmaster.metrics.services;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import aeza.hostmaster.metrics.dto.MetricDTO;
import aeza.hostmaster.metrics.models.AgentRating;
import aeza.hostmaster.metrics.models.Metric;
import aeza.hostmaster.metrics.repository.AgentRatingRepository;
import aeza.hostmaster.metrics.repository.MetricRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MetricService {
    private final AgentRatingRepository agentRatingRepository;
    private final MetricRepository metricRepository;

    public MetricService(MetricRepository metricRepository, AgentRatingRepository agentRatingRepository) {
        this.metricRepository = metricRepository;
        this.agentRatingRepository = agentRatingRepository;
    }
    public void saveAgentRating(AgentRating agentRating) {
        agentRatingRepository.save(agentRating);
    }
    public MetricDTO saveMetric(MetricDTO metricDTO) {
        Metric metric = metricDTO.toMetric();
        AgentRating agentRating;
        try{
            agentRating =  agentRatingRepository.findByAgentId((long) metricDTO.getAgentId()).orElseThrow(() -> new RuntimeException("AgentRating not found"));

        } catch (RuntimeException e) {
            agentRating = new AgentRating();
            agentRating.setAgentId(metricDTO.getAgentId());
            agentRating.setAgentName(metricDTO.getAgentName());
            saveAgentRating(agentRating);
        }
        metric.setAgentRating(agentRating);
        return metricRepository.save(metric).toDTO();
    }

    public AgentRatingDTO findAgent(Long agentId) {
        Optional<AgentRating> agentRating = agentRatingRepository.findByAgentId(agentId);

        if (agentRating.isPresent()) {
            return agentRating.get().toDTO();
        } else {
            throw new RuntimeException("Agent not found");
        }
    }
}
