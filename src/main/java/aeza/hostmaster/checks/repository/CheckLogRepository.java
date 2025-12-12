package aeza.hostmaster.checks.repository;

import aeza.hostmaster.checks.entity.CheckLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckLogRepository extends JpaRepository<CheckLogEntity, UUID> {}
