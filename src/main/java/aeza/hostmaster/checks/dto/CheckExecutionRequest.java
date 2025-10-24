package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import java.util.List;

public record CheckExecutionRequest(
        CheckType type,
        CheckStatus status,
        Long durationMillis,
        String message,
        HttpCheckDetailsDto httpDetails,
        PingCheckDetailsDto pingDetails,
        TcpCheckDetailsDto tcpDetails,
        TracerouteDetailsDto tracerouteDetails,
        DnsLookupDetailsDto dnsLookupDetails,
        List<CheckMetricDto> metrics
) {
}
