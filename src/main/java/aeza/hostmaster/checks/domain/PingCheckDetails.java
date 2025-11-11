package aeza.hostmaster.checks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PingCheckDetails {

    @Column(name = "ping_packets_transmitted")
    private Integer packetsTransmitted;

    @Column(name = "ping_packets_received")
    private Integer packetsReceived;

    @Column(name = "ping_packet_loss_pct")
    private Double packetLossPercentage;

    @Column(name = "ping_rtt_min_ms")
    private Double minimumRttMillis;

    @Column(name = "ping_rtt_avg_ms")
    private Double averageRttMillis;

    @Column(name = "ping_rtt_max_ms")
    private Double maximumRttMillis;

    @Column(name = "ping_rtt_stddev_ms")
    private Double standardDeviationRttMillis;

    public Integer getPacketsTransmitted() {
        return packetsTransmitted;
    }

    public void setPacketsTransmitted(Integer packetsTransmitted) {
        this.packetsTransmitted = packetsTransmitted;
    }

    public Integer getPacketsReceived() {
        return packetsReceived;
    }

    public void setPacketsReceived(Integer packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public Double getPacketLossPercentage() {
        return packetLossPercentage;
    }

    public void setPacketLossPercentage(Double packetLossPercentage) {
        this.packetLossPercentage = packetLossPercentage;
    }

    public Double getMinimumRttMillis() {
        return minimumRttMillis;
    }

    public void setMinimumRttMillis(Double minimumRttMillis) {
        this.minimumRttMillis = minimumRttMillis;
    }

    public Double getAverageRttMillis() {
        return averageRttMillis;
    }

    public void setAverageRttMillis(Double averageRttMillis) {
        this.averageRttMillis = averageRttMillis;
    }

    public Double getMaximumRttMillis() {
        return maximumRttMillis;
    }

    public void setMaximumRttMillis(Double maximumRttMillis) {
        this.maximumRttMillis = maximumRttMillis;
    }

    public Double getStandardDeviationRttMillis() {
        return standardDeviationRttMillis;
    }

    public void setStandardDeviationRttMillis(Double standardDeviationRttMillis) {
        this.standardDeviationRttMillis = standardDeviationRttMillis;
    }
}
