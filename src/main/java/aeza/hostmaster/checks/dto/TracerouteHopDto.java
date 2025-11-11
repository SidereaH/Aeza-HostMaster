package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TracerouteHopDto(
        @JsonAlias({"hopIndex", "hop", "index"})
        Integer hopIndex,
        @JsonAlias({"ipAddress", "ip", "ip_address"})
        String ipAddress,
        @JsonAlias({"hostname", "host"})
        String hostname,
        @JsonAlias({"latencyMillis", "latency", "rtt", "rtt_ms"})
        Long latencyMillis,
        @JsonAlias({"latitude", "lat"})
        Double latitude,
        @JsonAlias({"longitude", "lon", "lng"})
        Double longitude,
        @JsonAlias({"country", "country_name"})
        String country,
        @JsonAlias({"city", "location"})
        String city
) {
}
