package aeza.hostmaster.services;

import aeza.hostmaster.dto.MetricDTO;
import aeza.hostmaster.repository.AgentRepository;
import aeza.hostmaster.repository.MetricRepository;
import org.springframework.stereotype.Service;

@Service
public class MetricService {
    private MetricRepository metricRepository;
    private AgentRepository agentRepository;

    public MetricService(MetricRepository metricRepository, AgentRepository agentRepository) {
        this.metricRepository = metricRepository;
        this.agentRepository = agentRepository;
    }

    public void saveMetric(MetricDTO metricDTO) {
        metricRepository.save(metricDTO.toMetric());
    }

    public void findAgent(Long agentId) {
        agentRepository.findByAgentId(agentId);
    }
}
