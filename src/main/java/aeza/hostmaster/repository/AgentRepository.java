package aeza.hostmaster.repository;

import aeza.hostmaster.dto.AgentDTO;
import aeza.hostmaster.models.AgentRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<AgentRating, Long> {
    Optional<AgentRating> findByAgentId(Long agentId);
}
