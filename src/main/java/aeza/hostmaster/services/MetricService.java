package aeza.hostmaster.services;

import aeza.hostmaster.dto.MetricDTO;
import aeza.hostmaster.models.Metric;
import aeza.hostmaster.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class MetricService {
    private MetricRepository metricRepository;

    public MetricService(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public void saveMetric(MetricDTO metricDTO) {
        metricRepository.save(metricDTO.toMetric());
    }
}
