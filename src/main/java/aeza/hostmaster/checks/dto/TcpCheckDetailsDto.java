package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TcpCheckDetailsDto(
        @JsonAlias({"port", "target_port"})
        Integer port,
        @JsonAlias({"connectionTimeMillis", "connection_time", "connection_time_ms", "duration_ms"})
        Long connectionTimeMillis,
        @JsonAlias({"address", "ip", "ip_address", "host"})
        String address
) {
}
