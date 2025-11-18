package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PingCheckDetailsDto(
        @JsonAlias({"packetsTransmitted", "packets_sent", "sent"})
        Integer packetsTransmitted,
        @JsonAlias({"packetsReceived", "packets_received", "received"})
        Integer packetsReceived,
        @JsonAlias({"packetLossPercentage", "packet_loss", "loss_percent"})
        Double packetLossPercentage,
        @JsonAlias({"minimumRttMillis", "min", "min_rtt", "min_ms"})
        Double minimumRttMillis,
        @JsonAlias({"averageRttMillis", "avg", "average", "avg_rtt", "avg_ms"})
        Double averageRttMillis,
        @JsonAlias({"maximumRttMillis", "max", "max_rtt", "max_ms"})
        Double maximumRttMillis,
        @JsonAlias({"standardDeviationRttMillis", "stddev", "std_dev", "stddev_ms"})
        Double standardDeviationRttMillis
) {
}
