package aeza.hostmaster.checks.repository;

import aeza.hostmaster.checks.entity.SiteCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SiteCheckRepository extends JpaRepository<SiteCheckEntity, UUID> {}
