package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.domain.DnsRecordType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SiteCheckCreateRequest(
        @NotNull
        @Size(min = 1, max = 2048)
        String target,

        List<CheckType> checkTypes,

        // Дополнительные параметры для специфических проверок
        TcpCheckConfig tcpConfig,
        DnsLookupConfig dnsConfig,
        TracerouteConfig tracerouteConfig
) {

    public record TcpCheckConfig(
            Integer port,  // Порт для TCP проверки
            Integer timeoutMillis
    ) {}

    public record DnsLookupConfig(
            List<DnsRecordType> recordTypes,  // Типы DNS записей для проверки
            String dnsServer  // Специфический DNS сервер (опционально)
    ) {}

    public record TracerouteConfig(
            Integer maxHops,  // Максимальное количество хопов
            Integer timeoutMillis
    ) {}
}