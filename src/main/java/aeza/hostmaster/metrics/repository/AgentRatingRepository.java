package aeza.hostmaster.metrics.repository;

import aeza.hostmaster.metrics.models.AgentRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRatingRepository extends JpaRepository<AgentRating, Long> {
    Optional<AgentRating> findByAgentId(Long agentId);
}
