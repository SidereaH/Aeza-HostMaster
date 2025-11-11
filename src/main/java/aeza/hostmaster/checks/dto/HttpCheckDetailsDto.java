package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HttpCheckDetailsDto(
        @JsonAlias({"method", "http_method"})
        String method,
        @JsonAlias({"statusCode", "status", "status_code"})
        Integer statusCode,
        @JsonAlias({"responseTimeMillis", "response_time", "response_time_ms", "duration_ms"})
        Long responseTimeMillis,
        @JsonAlias({"headers", "response_headers"})
        Map<String, String> headers
) {
}
