package aeza.hostmaster.metrics.dto;

import aeza.hostmaster.metrics.models.Metric;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Schema(description = "DTO для передачи метрик агента")
public class MetricDTO {
    @Schema(description = "ID агента", example = "123", required = true)
    private Long agentId;
    @Schema(description = "Имя агента", example = "agent-1", required = true)
    private String agentName;
    @Schema(description = "Тип метрики", example = "Response_Delay", required = true)
    private Metric.MetricType metricType;
    @Schema(description = "Значение метрики", example = "45.5", required = true)
    private double value;
    @Schema(description = "Временная метка", example = "2023-10-01T12:00:00Z", required = true)
    private Instant timestamp;

    public Metric toMetric(){
        Metric m = new Metric();
        m.setAgentName(agentName);
        m.setMetricType(metricType);
        m.setValue(value);
//        m.setTimestamp(timestamp);
        return m;
    }
}
