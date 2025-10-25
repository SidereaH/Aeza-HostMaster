package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.domain.DnsRecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Payload describing a new site check job to schedule")
public record SiteCheckCreateRequest(
        @NotNull
        @Size(min = 1, max = 2048)
        @Schema(description = "Target hostname, IP address or URL that should be checked", example = "https://status.example.com")
        String target,

        @Schema(description = "Types of checks that should be executed for the target", implementation = CheckType.class)
        List<CheckType> checkTypes,

        @Schema(description = "TCP specific configuration", nullable = true)
        TcpCheckConfig tcpConfig,

        @Schema(description = "DNS lookup specific configuration", nullable = true)
        DnsLookupConfig dnsConfig,

        @Schema(description = "Traceroute specific configuration", nullable = true)
        TracerouteConfig tracerouteConfig
) {

    @Schema(description = "Options controlling how the TCP check is executed")
    public record TcpCheckConfig(
            @Schema(description = "Port that should be opened during the TCP check", example = "443")
            Integer port,

            @Schema(description = "Timeout for the TCP check in milliseconds", example = "5000")
            Integer timeoutMillis
    ) {}

    @Schema(description = "Options controlling DNS lookup checks")
    public record DnsLookupConfig(
            @Schema(description = "DNS record types that must be resolved", implementation = DnsRecordType.class)
            List<DnsRecordType> recordTypes,

            @Schema(description = "Custom DNS resolver that should be queried", example = "8.8.8.8")
            String dnsServer
    ) {}

    @Schema(description = "Options controlling traceroute execution")
    public record TracerouteConfig(
            @Schema(description = "Maximum number of hops to traverse", example = "30")
            Integer maxHops,

            @Schema(description = "Timeout for a single traceroute hop in milliseconds", example = "2000")
            Integer timeoutMillis
    ) {}
}