package aeza.hostmaster.metrics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO с рейтингом и статистикой агента")
public class AgentRatingDTO {
    @Schema(description = "Имя агента", example = "agent-1")
    private String agentName;
    @Schema(description = "Средняя задержка", example = "25.5")
    private double averageLatency;
    @Schema(description = "Средняя доступность", example = "99.8")
    private double averageAvailability;
    @Schema(description = "Общее количество запросов", example = "150")
    private int totalRequestCount;
}
