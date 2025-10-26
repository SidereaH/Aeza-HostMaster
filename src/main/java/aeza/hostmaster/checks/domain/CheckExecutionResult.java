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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "check_execution_results")
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

    public UUID getId() {
        return id;
    }

    public CheckType getType() {
        return type;
    }

    public void setType(CheckType type) {
        this.type = type;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public void setStatus(CheckStatus status) {
        this.status = status;
    }

    public Long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(Long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpCheckDetails getHttpDetails() {
        return httpDetails;
    }

    public void setHttpDetails(HttpCheckDetails httpDetails) {
        this.httpDetails = httpDetails;
    }

    public PingCheckDetails getPingDetails() {
        return pingDetails;
    }

    public void setPingDetails(PingCheckDetails pingDetails) {
        this.pingDetails = pingDetails;
    }

    public TcpCheckDetails getTcpDetails() {
        return tcpDetails;
    }

    public void setTcpDetails(TcpCheckDetails tcpDetails) {
        this.tcpDetails = tcpDetails;
    }

    public TracerouteDetails getTracerouteDetails() {
        return tracerouteDetails;
    }

    public void setTracerouteDetails(TracerouteDetails tracerouteDetails) {
        this.tracerouteDetails = tracerouteDetails;
    }

    public DnsLookupDetails getDnsLookupDetails() {
        return dnsLookupDetails;
    }

    public void setDnsLookupDetails(DnsLookupDetails dnsLookupDetails) {
        this.dnsLookupDetails = dnsLookupDetails;
    }

    public List<CheckMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<CheckMetric> metrics) {
        this.metrics = metrics;
    }

    public SiteCheckResult getSiteCheckResult() {
        return siteCheckResult;
    }

    public void setSiteCheckResult(SiteCheckResult siteCheckResult) {
        this.siteCheckResult = siteCheckResult;
    }
}
