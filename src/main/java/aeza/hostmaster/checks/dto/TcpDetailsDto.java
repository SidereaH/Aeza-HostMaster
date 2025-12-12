package aeza.hostmaster.checks.dto;

public record TcpDetailsDto(
        String location,
        String country,
        Long connectTimeMillis,
        String status,
        String ip
) {}
