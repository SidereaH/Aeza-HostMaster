package aeza.hostmaster.checks.dto;

public record PingCheckDetailsDto(
        Integer packetsTransmitted,
        Integer packetsReceived,
        Double packetLossPercentage,
        Long minimumRttMillis,
        Long averageRttMillis,
        Long maximumRttMillis,
        Long standardDeviationRttMillis
) {
}
