package aeza.hostmaster.checks.dto;

public record TracerouteHopDto(
        Integer hopIndex,
        String ipAddress,
        String hostname,
        Long latencyMillis,
        Double latitude,
        Double longitude,
        String country,
        String city
) {
}
