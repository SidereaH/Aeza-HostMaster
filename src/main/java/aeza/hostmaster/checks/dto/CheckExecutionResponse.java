package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckExecutionResponse(
        UUID id,
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
