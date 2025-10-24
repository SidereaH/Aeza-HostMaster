package aeza.hostmaster.checks.repository;

import aeza.hostmaster.checks.domain.SiteCheckResult;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteCheckResultRepository extends JpaRepository<SiteCheckResult, UUID> {

    Page<SiteCheckResult> findByTargetContainingIgnoreCase(String target, Pageable pageable);
}
