package aeza.hostmaster.agents.controller;


import aeza.hostmaster.agents.dto.AgentDTO;
import aeza.hostmaster.agents.dto.AgentRegistrationRequest;
import aeza.hostmaster.agents.services.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Register a new agent. Returns raw token only in response body (one-time).
     */
    @PostMapping("/register")
    public ResponseEntity<AgentDTO> register(@RequestBody AgentRegistrationRequest req) {
        AgentDTO dto = agentService.registerAgent(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Heartbeat by id (protected endpoint in real app). Updates lastHeartbeat.
     */
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<AgentDTO> heartbeat(@PathVariable Long id) {
        AgentDTO dto = agentService.updateHeartbeat(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Heartbeat by name+token in body - useful for simpler clients.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<AgentDTO> heartbeatByName(@RequestParam String agentName, @RequestParam String token) {
        AgentDTO dto = agentService.heartbeatByNameAndToken(agentName, token);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgent(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<AgentDTO> getByName(@PathVariable String name) {
        return ResponseEntity.ok(agentService.getAgentByName(name));
    }

    @GetMapping
    public ResponseEntity<Page<AgentDTO>> list(Pageable pageable) {
        return ResponseEntity.ok(agentService.listAgents(pageable));
    }

    @PostMapping("/{id}/rotate-token")
    public ResponseEntity<AgentDTO> rotateToken(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.rotateToken(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validate(@RequestParam String agentName, @RequestParam String token) {
        boolean ok = agentService.validateToken(agentName, token);
        return ResponseEntity.ok(ok);
    }
}

