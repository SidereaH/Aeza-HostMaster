package aeza.hostmaster.metrics.services;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import aeza.hostmaster.metrics.dto.MetricDTO;
import aeza.hostmaster.metrics.models.AgentRating;
import aeza.hostmaster.metrics.models.Metric;
import aeza.hostmaster.metrics.repository.AgentRatingRepository;
import aeza.hostmaster.metrics.repository.MetricRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
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
    @Transactional
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
        metric.setAgentRating(agentRatingRepository.save(agentRating));

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

    public void agentCounter(Long agentId) {
        try {
            AgentRating agentRating = agentRatingRepository.findByAgentId(agentId).orElseThrow(() -> new RuntimeException("agent info not found"));
            agentRating.setTotalRequestCount(agentRating.getTotalRequestCount() + 1);
            agentRatingRepository.save(agentRating);
        } catch (RuntimeException e) {
            throw new RuntimeException("Agent info not found");
        }
    }

    public void averageLatency(Long agentId) {
        try {
            AgentRating agentRating = agentRatingRepository.findByAgentId(agentId).orElseThrow(() -> new RuntimeException("agent info not found"));
            List<Metric> metrics = metricRepository.findAllByAgentRatingAndMetricType(agentRating, Metric.MetricType.RESPONSE_DELAY);
            double sum = 0L;
            for(Metric m : metrics){
                sum+=m.getValue();
            }
            double avg = sum / metrics.size();
            avg = (double) Math.round(avg * 100) / 100;
            agentRating.setAverageLatency(avg);
            agentRatingRepository.save(agentRating);
        } catch (RuntimeException e) {
            throw new RuntimeException("Agent info not found");
        }

    }

    public int[] availabilityWindow = new int[100];
    private int currentIndex = 0;

    public void addAvailabilityCheck(int available, Long agentId) {
        try {
            AgentRating agentRating = agentRatingRepository.findByAgentId(agentId).orElseThrow(() -> new RuntimeException("agent info not found"));
//                    {
//                        AgentRating newRating = new AgentRating();
//                        newRating.setAgentId(agentId);
//                        return newRating;
//                    });

            availabilityWindow[currentIndex] = available;
            currentIndex = (currentIndex + 1) % availabilityWindow.length;

            double sum = 0;
            for (int value : availabilityWindow) {
                sum += value;
            }
            double res = sum / availabilityWindow.length * 100;
//            log.info(String.valueOf(res));
            agentRating.setAverageAvailability(res);
            agentRatingRepository.save(agentRating);
        } catch (RuntimeException e) {
            throw new RuntimeException("Agent not found");
        }
    }
}
