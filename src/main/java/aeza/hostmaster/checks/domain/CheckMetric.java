package aeza.hostmaster.checks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Simple metric representation collected during a check execution.
 */
@Embeddable
public class CheckMetric {

    @Column(name = "metric_name", nullable = false)
    private String name;

    @Column(name = "metric_value")
    private Double value;

    @Column(name = "metric_unit")
    private String unit;

    @Column(name = "metric_description")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
