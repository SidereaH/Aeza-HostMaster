package aeza.hostmaster.checks.repository;

import aeza.hostmaster.checks.entity.SiteCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteCheckRepository extends JpaRepository<SiteCheckEntity, UUID> {
}