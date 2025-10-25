package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckType;

import java.util.List;
import java.util.UUID;

public record SiteCheckTask(
        UUID taskId,
        String target,
        String callbackTopic,
        List<CheckType> checkTypes,
        SiteCheckCreateRequest.TcpCheckConfig tcpConfig,
        SiteCheckCreateRequest.DnsLookupConfig dnsConfig,
        SiteCheckCreateRequest.TracerouteConfig tracerouteConfig
) {}