package aeza.hostmaster.checks.dto;

import java.util.Map;

public record HttpCheckDetailsDto(
        String method,
        Integer statusCode,
        Long responseTimeMillis,
        Map<String, String> headers
) {
}
