package aeza.hostmaster.metrics.repository;

import aeza.hostmaster.metrics.models.AgentRating;
import aeza.hostmaster.metrics.models.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
    List<Metric> findAllByAgentRatingAndMetricType(AgentRating agentRating, Metric.MetricType metricType);
}
