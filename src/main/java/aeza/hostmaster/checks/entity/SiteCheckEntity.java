package aeza.hostmaster.checks.entity;

import aeza.hostmaster.checks.domain.CheckStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "site_checks")
public class SiteCheckEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String target;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "total_duration_millis")
    private Long totalDurationMillis;

    @OneToMany(mappedBy = "siteCheck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CheckExecutionEntity> checks = new ArrayList<>();

    public SiteCheckEntity() {}

    public SiteCheckEntity(UUID id, String target, Instant executedAt, CheckStatus status) {
        this.id = id;
        this.target = target;
        this.executedAt = executedAt;
        this.status = status;
    }

    public SiteCheckEntity(UUID id, String target, Instant executedAt, CheckStatus status, Long totalDurationMillis) {
        this.id = id;
        this.target = target;
        this.executedAt = executedAt;
        this.status = status;
        this.totalDurationMillis = totalDurationMillis;
    }

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public CheckStatus getStatus() { return status; }
    public void setStatus(CheckStatus status) { this.status = status; }

    public Long getTotalDurationMillis() { return totalDurationMillis; }
    public void setTotalDurationMillis(Long totalDurationMillis) { this.totalDurationMillis = totalDurationMillis; }

    public List<CheckExecutionEntity> getChecks() { return checks; }
    public void setChecks(List<CheckExecutionEntity> checks) { this.checks = checks; }

    public void addCheck(CheckExecutionEntity check) {
        checks.add(check);
        check.setSiteCheck(this);
    }
}