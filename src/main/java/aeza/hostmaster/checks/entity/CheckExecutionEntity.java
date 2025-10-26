package aeza.hostmaster.checks.entity;

import aeza.hostmaster.checks.domain.CheckType;  // Из domain
import aeza.hostmaster.checks.domain.CheckStatus; // Из domain
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "check_executions")
public class CheckExecutionEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "duration_millis", nullable = false)
    private Long durationMillis;

    @Column(columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_check_id", nullable = false)
    private SiteCheckEntity siteCheck;

    @OneToOne(mappedBy = "checkExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private HttpDetailsEntity httpDetails;

    @OneToMany(mappedBy = "checkExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CheckMetricEntity> metrics = new ArrayList<>();

    public CheckExecutionEntity() {}

    public CheckExecutionEntity(UUID id, CheckType type, CheckStatus status, Long durationMillis, String message) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.durationMillis = durationMillis;
        this.message = message;
    }

    // Геттеры и сеттеры остаются без изменений
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CheckType getType() { return type; }
    public void setType(CheckType type) { this.type = type; }

    public CheckStatus getStatus() { return status; }
    public void setStatus(CheckStatus status) { this.status = status; }

    public Long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(Long durationMillis) { this.durationMillis = durationMillis; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SiteCheckEntity getSiteCheck() { return siteCheck; }
    public void setSiteCheck(SiteCheckEntity siteCheck) { this.siteCheck = siteCheck; }

    public HttpDetailsEntity getHttpDetails() { return httpDetails; }
    public void setHttpDetails(HttpDetailsEntity httpDetails) {
        this.httpDetails = httpDetails;
        if (httpDetails != null) {
            httpDetails.setCheckExecution(this);
        }
    }

    public List<CheckMetricEntity> getMetrics() { return metrics; }
    public void setMetrics(List<CheckMetricEntity> metrics) {
        this.metrics = metrics;
        if (metrics != null) {
            for (CheckMetricEntity metric : metrics) {
                metric.setCheckExecution(this);
            }
        }
    }

    public void addMetric(CheckMetricEntity metric) {
        metrics.add(metric);
        metric.setCheckExecution(this);
    }
}