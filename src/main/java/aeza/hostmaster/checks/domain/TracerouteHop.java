package aeza.hostmaster.checks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TracerouteHop {

    @Column(name = "hop_index")
    private Integer hopIndex;

    @Column(name = "hop_ip")
    private String ipAddress;

    @Column(name = "hop_hostname")
    private String hostname;

    @Column(name = "hop_latency_ms")
    private Long latencyMillis;

    @Column(name = "hop_latitude")
    private Double latitude;

    @Column(name = "hop_longitude")
    private Double longitude;

    @Column(name = "hop_country")
    private String country;

    @Column(name = "hop_city")
    private String city;

    public Integer getHopIndex() {
        return hopIndex;
    }

    public void setHopIndex(Integer hopIndex) {
        this.hopIndex = hopIndex;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Long getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(Long latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
