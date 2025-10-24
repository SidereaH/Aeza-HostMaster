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
    private Long minimumRttMillis;

    @Column(name = "ping_rtt_avg_ms")
    private Long averageRttMillis;

    @Column(name = "ping_rtt_max_ms")
    private Long maximumRttMillis;

    @Column(name = "ping_rtt_stddev_ms")
    private Long standardDeviationRttMillis;

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

    public Long getMinimumRttMillis() {
        return minimumRttMillis;
    }

    public void setMinimumRttMillis(Long minimumRttMillis) {
        this.minimumRttMillis = minimumRttMillis;
    }

    public Long getAverageRttMillis() {
        return averageRttMillis;
    }

    public void setAverageRttMillis(Long averageRttMillis) {
        this.averageRttMillis = averageRttMillis;
    }

    public Long getMaximumRttMillis() {
        return maximumRttMillis;
    }

    public void setMaximumRttMillis(Long maximumRttMillis) {
        this.maximumRttMillis = maximumRttMillis;
    }

    public Long getStandardDeviationRttMillis() {
        return standardDeviationRttMillis;
    }

    public void setStandardDeviationRttMillis(Long standardDeviationRttMillis) {
        this.standardDeviationRttMillis = standardDeviationRttMillis;
    }
}
