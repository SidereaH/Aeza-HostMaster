package aeza.hostmaster.checks.dto;

public record PingDetailsDto(
        String location,
        String country,
        String ip,
        Integer transmitted,
        Integer received,
        Double packetLoss,
        Double minRtt,
        Double avgRtt,
        Double maxRtt
) {}
