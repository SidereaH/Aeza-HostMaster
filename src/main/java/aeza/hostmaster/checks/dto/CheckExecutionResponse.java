package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CheckExecutionResponse(
        UUID id,
        CheckType type,
        CheckStatus status,
        Long durationMillis,
        String message,
        HttpDetailsDto http,
        PingDetailsDto ping,
        TcpDetailsDto tcp,
        TracerouteDetailsDto traceroute,
        DnsDetailsDto dns
) {}
