package aeza.hostmaster.metrics.controllers;

import aeza.hostmaster.metrics.dto.AgentRatingDTO;
import aeza.hostmaster.metrics.dto.MetricDTO;
import aeza.hostmaster.metrics.services.MetricService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Metrics API", description = "API для работы с метриками агентов")
public class MetricController {
    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @Operation(
            summary = "Отправить метрики агента",
            description = "Принимает метрики от агента, сохраняет их и обновляет счетчики"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метрики успешно приняты"),
            @ApiResponse(responseCode = "400", description = "Неверные данные метрик"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping("/metric")
    public ResponseEntity<?> receivedMetrics(
            @Parameter(description = "DTO с метриками агента", required = true)
            @RequestBody MetricDTO metricDTO) {

        int available = 1;
        metricService.saveMetric(metricDTO);
        metricService.agentCounter(metricDTO.getAgentId());
        metricService.addAvailabilityCheck(available, metricDTO.getAgentId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metricDTO);
    }

    @Operation(
            summary = "Получить метрики агента",
            description = "Возвращает статистику и рейтинг агента по его ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные агента успешно получены"),
            @ApiResponse(responseCode = "404", description = "Агент не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<?> getAgentMetrics(
            @Parameter(description = "ID агента", example = "123", required = true)
            @PathVariable Long agentId) {

//        metricService.findAgent(agentDTO);
        metricService.averageLatency(agentId);
        AgentRatingDTO agentRatingDTO = metricService.findAgent(agentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(agentRatingDTO);
    }

}
