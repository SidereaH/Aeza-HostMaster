package aeza.hostmaster.checks.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_logs")
@Getter
@Setter
public class CheckLogEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_check_id")
    @JsonIgnore
    private SiteCheckEntity siteCheck;

    @Column(columnDefinition = "TEXT")
    private String rawJson;

    @Column
    private Instant timestamp;

    public CheckLogEntity() {}
}
