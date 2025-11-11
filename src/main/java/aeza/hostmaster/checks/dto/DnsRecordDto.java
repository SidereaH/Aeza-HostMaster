package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.DnsRecordType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DnsRecordDto(
        @JsonAlias({"type", "record_type"})
        DnsRecordType type,
        @JsonAlias({"value", "record", "answer"})
        String value,
        @JsonAlias({"ttl", "time_to_live"})
        Long ttl
) {
}
