package aeza.hostmaster.checks.dto;


import java.util.Map;

public record HttpDetailsDto(
        String location,
        String country,
        Long timeMillis,
        Integer statusCode,
        String ip,
        String result,
        Map<String, String> headers
) {}
