package aeza.hostmaster.checks.dto;

import java.util.List;

public record DnsDetailsDto(
        String location,
        String country,
        List<String> records,
        String ttl
) {}
