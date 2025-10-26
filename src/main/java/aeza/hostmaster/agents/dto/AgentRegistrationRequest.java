package aeza.hostmaster.agents.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Запрос на регистрацию агента")
@AllArgsConstructor
@NoArgsConstructor
public class AgentRegistrationRequest {

    @Schema(description = "Имя агента", example = "europe-agent-01", required = true)
    private String agentName;

    @Schema(description = "IP адрес агента", example = "192.168.1.100", required = true)
    private String ipAddress;

    @Schema(description = "Страна расположения агента", example = "RU", required = true)
    private String country;
}
