package aeza.hostmaster.checks.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "site_check_results")
public class SiteCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "target", nullable = false)
    private String target;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private CheckStatus status;

    @Column(name = "total_duration_millis", nullable = false)
    private Long totalDurationMillis;

    @OneToMany(mappedBy = "siteCheckResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id")
    private List<CheckExecutionResult> checks = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public void setStatus(CheckStatus status) {
        this.status = status;
    }

    public Long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public void setTotalDurationMillis(Long totalDurationMillis) {
        this.totalDurationMillis = totalDurationMillis;
    }

    public List<CheckExecutionResult> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckExecutionResult> checks) {
        this.checks = checks;
    }

    public void addCheck(CheckExecutionResult executionResult) {
        executionResult.setSiteCheckResult(this);
        checks.add(executionResult);
    }
}
