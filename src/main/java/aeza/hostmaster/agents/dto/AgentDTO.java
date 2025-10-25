package aeza.hostmaster.agents.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Schema(description = "DTO агента")
public class AgentDTO {

    @Schema(description = "ID агента", example = "1")
    private Long id;

    @Schema(description = "Имя агента", example = "europe-agent-01")
    private String agentName;

    @Schema(description = "IP адрес агента", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "Страна расположения", example = "RU")
    private String country;

    @Schema(description = "Токен агента (отображается только при регистрации или смене токена)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String agentToken;

    @Schema(description = "Время создания агента")
    private OffsetDateTime createdAt;

    @Schema(description = "Время последнего обновления")
    private OffsetDateTime updatedAt;

    @Schema(description = "Время последнего heartbeat")
    private OffsetDateTime lastHeartbeat;

    @Schema(description = "Статус агента", example = "ACTIVE")
    private String status;
}