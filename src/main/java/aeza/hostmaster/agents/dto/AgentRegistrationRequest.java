package aeza.hostmaster.agents.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentRegistrationRequest {
    private String agentName;
    private String ipAddress;
    private String agentCountry;
}
