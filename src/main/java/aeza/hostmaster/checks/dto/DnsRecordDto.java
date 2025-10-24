package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.DnsRecordType;

public record DnsRecordDto(
        DnsRecordType type,
        String value,
        Long ttl
) {
}
