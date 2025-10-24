package aeza.hostmaster.checks.dto;

public record CheckMetricDto(
        String name,
        Double value,
        String unit,
        String description
) {
}
