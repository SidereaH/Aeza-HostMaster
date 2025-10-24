package aeza.hostmaster.checks.dto;

public record TcpCheckDetailsDto(
        Integer port,
        Long connectionTimeMillis,
        String address
) {
}
