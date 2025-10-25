package aeza.hostmaster.agents.controller;

import aeza.hostmaster.agents.dto.AgentDTO;
import aeza.hostmaster.agents.dto.AgentRegistrationRequest;
import aeza.hostmaster.agents.models.Agent;
import aeza.hostmaster.agents.services.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Tag(name = "Agents Management", description = "API для управления агентами проверки доступности")
public class AgentController {

    private final AgentService agentService;
    @GetMapping("/docs")
    @Operation(hidden = true)
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html?url=/v3/api-docs#/Agents%20Management";
    }
    @Operation(
            summary = "Регистрация нового агента",
            description = "Создает нового агента и возвращает токен для аутентификации. Токен отображается только один раз."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Агент успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = AgentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Неверные входные данные")
    })
    @PostMapping("/register")
    public ResponseEntity<AgentDTO> register(@RequestBody AgentRegistrationRequest req) {
        AgentDTO dto = agentService.registerAgent(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(
            summary = "Обновление heartbeat по ID",
            description = "Обновляет время последней активности агента по его идентификатору",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Heartbeat обновлен"),
            @ApiResponse(responseCode = "404", description = "Агент не найден"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<AgentDTO> heartbeat(
            @Parameter(description = "ID агента") @PathVariable Long id) {
        AgentDTO dto = agentService.updateHeartbeat(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Обновление heartbeat по имени и токену",
            description = "Обновляет время последней активности аутентифицированного агента",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Heartbeat обновлен"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен")
    })
    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<AgentDTO> heartbeatByName(@AuthenticationPrincipal Agent authenticatedAgent) {
        AgentDTO dto = agentService.heartbeatAuthenticated(authenticatedAgent.getAgentName());
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Получить агента по ID",
            description = "Возвращает информацию об агенте по его идентификатору",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Агент найден"),
            @ApiResponse(responseCode = "404", description = "Агент не найден")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<AgentDTO> get(
            @Parameter(description = "ID агента") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal instanceof Agent agent && !agent.getId().equals(id)) {
            throw new AccessDeniedException("Agents may only access their own profile");
        }
        return ResponseEntity.ok(agentService.getAgent(id));
    }

    @Operation(
            summary = "Получить агента по имени",
            description = "Возвращает информацию об агенте по его имени",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Агент найден"),
            @ApiResponse(responseCode = "404", description = "Агент не найден")
    })
    @GetMapping("/by-name/{name}")
    public ResponseEntity<AgentDTO> getByName(
            @Parameter(description = "Имя агента") @PathVariable String name) {
        return ResponseEntity.ok(agentService.getAgentByName(name));
    }

    @Operation(
            summary = "Список агентов",
            description = "Возвращает страницу с списком всех агентов",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponse(responseCode = "200", description = "Список агентов получен")
    @GetMapping
    public ResponseEntity<Page<AgentDTO>> list(
            @Parameter(description = "Параметры пагинации") Pageable pageable) {
        return ResponseEntity.ok(agentService.listAgents(pageable));
    }

    @Operation(
            summary = "Смена токена агента",
            description = "Генерирует новый токен для агента. Старый токен становится недействительным.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен успешно изменен"),
            @ApiResponse(responseCode = "404", description = "Агент не найден")
    })
    @PostMapping("/{id}/rotate-token")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<AgentDTO> rotateToken(
            @Parameter(description = "ID агента") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal instanceof Agent agent && !agent.getId().equals(id)) {
            throw new AccessDeniedException("Agents may only rotate their own token");
        }
        return ResponseEntity.ok(agentService.rotateToken(id));
    }

    @Operation(
            summary = "Удаление агента",
            description = "Удаляет агента по его идентификатору",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Агент успешно удален"),
            @ApiResponse(responseCode = "404", description = "Агент не найден")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID агента") @PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Проверка токена агента",
            description = "Проверяет валидность пары имя агента/токен"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен проверен",
                    content = @Content(schema = @Schema(type = "boolean"))),
    })
    @GetMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> validate(
            @Parameter(description = "Имя агента") @RequestParam String agentName,
            @Parameter(description = "Токен для проверки") @RequestParam String token) {
        boolean ok = agentService.validateToken(agentName, token);
        return ResponseEntity.ok(ok);
    }
}