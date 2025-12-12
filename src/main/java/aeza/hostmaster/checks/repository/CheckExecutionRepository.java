package aeza.hostmaster.checks.repository;

import aeza.hostmaster.checks.entity.CheckExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CheckExecutionRepository extends JpaRepository<CheckExecutionEntity, UUID> {

    List<CheckExecutionEntity> findBySiteCheckId(UUID siteCheckId);

}
