package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckExecutionResponse(
        @JsonAlias({"id", "check_id", "execution_id"})
        UUID id,
        @JsonAlias({"type", "checkType", "check_type"})
        CheckType type,
        @JsonAlias({"status", "result"})
        CheckStatus status,
        @JsonAlias({"durationMillis", "duration", "duration_ms", "elapsed_ms"})
        Long durationMillis,
        @JsonAlias({"message", "error", "description"})
        String message,
        @JsonAlias({"httpDetails", "http", "http_details"})
        HttpCheckDetailsDto httpDetails,
        @JsonAlias({"pingDetails", "ping", "ping_details"})
        PingCheckDetailsDto pingDetails,
        @JsonAlias({"tcpDetails", "tcp", "tcp_details"})
        TcpCheckDetailsDto tcpDetails,
        @JsonAlias({"tracerouteDetails", "traceroute", "traceroute_details"})
        TracerouteDetailsDto tracerouteDetails,
        @JsonAlias({"dnsLookupDetails", "dns", "dns_details", "dns_lookup"})
        DnsLookupDetailsDto dnsLookupDetails,
        @JsonAlias({"metrics", "measurements"})
        List<CheckMetricDto> metrics
) {
}
