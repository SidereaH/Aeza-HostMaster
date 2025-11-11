package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckMetricDto(
        @JsonAlias({"name", "metric", "key"})
        String name,
        @JsonAlias({"value", "metric_value"})
        Double value,
        @JsonAlias({"unit", "metric_unit"})
        String unit,
        @JsonAlias({"description", "details"})
        String description
) {
}
