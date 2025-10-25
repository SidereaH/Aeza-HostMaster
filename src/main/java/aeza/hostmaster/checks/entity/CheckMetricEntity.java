package aeza.hostmaster.checks.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "check_metrics")
public class CheckMetricEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private Double value;

    private String unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_execution_id", nullable = false)
    private CheckExecutionEntity checkExecution;

    public CheckMetricEntity() {}

    public CheckMetricEntity(UUID id, String name, Double value, String unit, String description) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.description = description;
    }

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CheckExecutionEntity getCheckExecution() { return checkExecution; }
    public void setCheckExecution(CheckExecutionEntity checkExecution) { this.checkExecution = checkExecution; }
}