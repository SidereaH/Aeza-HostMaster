package aeza.hostmaster.checks.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "check_execution_results")
@Getter
@Setter

public class CheckExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false)
    private CheckType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false)
    private CheckStatus status;

    @Column(name = "duration_millis", nullable = false)
    private Long durationMillis;

    @Column(name = "check_message", length = 2048)
    private String message;

    @Embedded
    private HttpCheckDetails httpDetails;

    @Embedded
    private PingCheckDetails pingDetails;

    @Embedded
    private TcpCheckDetails tcpDetails;

    @Setter
    @Embedded
    private TracerouteDetails tracerouteDetails;

    @Embedded
    private DnsLookupDetails dnsLookupDetails;

    @ElementCollection
    @CollectionTable(name = "check_metrics", joinColumns = @JoinColumn(name = "execution_result_id"))
    private List<CheckMetric> metrics = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "site_check_result_id")
    private SiteCheckResult siteCheckResult;
}
