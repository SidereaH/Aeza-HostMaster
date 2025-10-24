package aeza.hostmaster.checks.dto;

import java.util.List;

public record DnsLookupDetailsDto(List<DnsRecordDto> records) {
}
