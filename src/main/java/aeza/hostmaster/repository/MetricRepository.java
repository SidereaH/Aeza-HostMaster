package aeza.hostmaster.repository;

import aeza.hostmaster.models.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {
}
