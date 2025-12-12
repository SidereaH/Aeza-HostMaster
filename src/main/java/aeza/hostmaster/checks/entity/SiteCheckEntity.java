package aeza.hostmaster.checks.entity;

import aeza.hostmaster.checks.domain.CheckStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "site_checks")
@Getter
@Setter
public class SiteCheckEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private Instant executedAt;

    @Column
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "total_duration_millis")
    private Long totalDurationMillis;

    @OneToMany(mappedBy = "siteCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckExecutionEntity> executions = new ArrayList<>();

    @OneToMany(mappedBy = "siteCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CheckLogEntity> logs = new ArrayList<>();

    public SiteCheckEntity() {}

    public SiteCheckEntity(UUID id, String target, Instant executedAt, CheckStatus status) {
        this.id = id;
        this.target = target;
        this.executedAt = executedAt;
        this.status = status;
    }

}
