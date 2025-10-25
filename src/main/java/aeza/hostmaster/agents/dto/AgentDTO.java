package aeza.hostmaster.agents.dto;


import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgentDTO {
    private Long id;
    private String agentName;
    private String ipAddress;
    private String agentCountry;
    private String agentToken; // возвращаем токен при регистрации/ротации; не включаем в публичные списки
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastHeartbeat;
    private String status;
}
