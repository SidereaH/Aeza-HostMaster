package aeza.hostmaster.agents.repositories;

import aeza.hostmaster.agents.models.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByAgentName(String name);
    boolean existsByAgentName(String name);
    boolean existsByIpAddress(String ipAddress);
    List<Agent> findByStatus(Agent.Status status);
    Page<Agent> findAll(Pageable pageable);
}

