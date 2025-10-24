package aeza.hostmaster.checks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TcpCheckDetails {

    @Column(name = "tcp_port")
    private Integer port;

    @Column(name = "tcp_connection_time_ms")
    private Long connectionTimeMillis;

    @Column(name = "tcp_address")
    private String address;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getConnectionTimeMillis() {
        return connectionTimeMillis;
    }

    public void setConnectionTimeMillis(Long connectionTimeMillis) {
        this.connectionTimeMillis = connectionTimeMillis;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
