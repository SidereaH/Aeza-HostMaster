package aeza.hostmaster.checks.dto;

public record TracerouteHopDto(
        int hop,
        String ip,
        String time
) {}
