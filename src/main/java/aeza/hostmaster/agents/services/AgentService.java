package aeza.hostmaster.agents.services;

import aeza.hostmaster.agents.dto.AgentDTO;
import aeza.hostmaster.agents.dto.AgentRegistrationRequest;
import aeza.hostmaster.agents.exceptions.AgentAlreadyExistsException;
import aeza.hostmaster.agents.exceptions.AgentNotFoundException;
import aeza.hostmaster.agents.models.Agent;
import aeza.hostmaster.agents.repositories.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AgentService implements UserDetailsService {
    private final AgentRepository agentRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Регистрация нового агента. Генерируем токен и сохраняем сущность.
     * Выбрасывает AgentAlreadyExistsException если имя занято.
     */
    // Регистрация агента: возвращаем DTO с raw token (только один раз)
    public AgentDTO registerAgent(AgentRegistrationRequest req) {
        if (agentRepo.existsByAgentName(req.getAgentName())) {
            throw new AgentAlreadyExistsException("Agent already exists");
        }
        String rawToken = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(rawToken);

        Agent a = new Agent();
        a.setAgentName(req.getAgentName());
        a.setIpAddress(req.getIpAddress());
        a.setAgentCountry(req.getCountry());
        a.setAgentToken(hashed); // хеш в БД
        a = agentRepo.save(a);

        AgentDTO dto = toDto(a, false);
        dto.setAgentToken(rawToken); // возвращаем raw token клиенту только один раз (регистрация)
        return dto;
    }

    // Ротация токена: создаём новый raw token, хешируем, сохраняем - возвращаем raw клиенту
    public AgentDTO rotateToken(Long agentId) {
        Agent agent = agentRepo.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentId));
        String newRaw = generateToken();
        String newHashed = passwordEncoder.encode(newRaw);
        agent.setAgentToken(newHashed);
        agentRepo.save(agent);

        AgentDTO dto = toDto(agent, true);
        dto.setAgentToken(newRaw); // вернуть новый raw токен (один раз)
        return dto;
    }

    // Валидация токена: по имени агента + raw token
    @Transactional(readOnly = true)
    public boolean validateToken(String agentName, String rawToken) {
        return agentRepo.findByAgentName(agentName)
                .map(agent -> passwordEncoder.matches(rawToken, agent.getAgentToken()))
                .orElse(false);
    }

    // Heartbeat по имени+token (пример защищённого эндпоинта)
    public AgentDTO heartbeatByNameAndToken(String agentName, String rawToken) {
        Agent agent = agentRepo.findByAgentName(agentName)
                .orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentName));

        if (!passwordEncoder.matches(rawToken, agent.getAgentToken())) {
            throw new BadCredentialsException("Invalid token for agent: " + agentName);
        }

        agent.setLastHeartbeat(OffsetDateTime.now());
        agentRepo.save(agent);
        return toDto(agent, false);
    }




    /**
     * Обновление heartbeat от агента (например, при /agents/{id}/heartbeat).
     * Возвращает DTO агента
     */
    public AgentDTO updateHeartbeat(Long agentId) {
        Agent agent = agentRepo.findById(agentId).orElseThrow(() -> new AgentNotFoundException("Agent not found: " + agentId));
        agent.setLastHeartbeat(OffsetDateTime.now());
        agentRepo.save(agent);
        return toDto(agent, false);
    }

    /**
     * Обновление heartbeat по токену (полезно, если агент не знает свой id)
     */
//    public AgentDTO updateHeartbeatByToken(String token) {
//        Agent agent = agentRepo.findByAgentToken(token).orElseThrow(() -> new AgentNotFoundException("Agent with token not found"));
//        agent.setLastHeartbeat(OffsetDateTime.now());
//        agentRepo.save(agent);
//        return toDto(agent, false);
//    }

    /** Найти агента по id */
    @Transactional(readOnly = true)
    public AgentDTO getAgent(Long id) {
        return agentRepo.findById(id).map(a -> toDto(a, false)).orElseThrow(() -> new AgentNotFoundException("Agent not found: " + id));
    }

    /** Найти агента по имени */
    @Transactional(readOnly = true)
    public AgentDTO getAgentByName(String name) {
        return agentRepo.findByAgentName(name).map(a -> toDto(a, false)).orElseThrow(() -> new AgentNotFoundException("Agent not found by name: " + name));
    }

    /** Найти по токену (например при авторизации агента) */
//    @Transactional(readOnly = true)
//    public AgentDTO getAgentByToken(String token) {
//        return agentRepo.findByAgentToken(token).map(a -> toDto(a, false)).orElseThrow(() -> new AgentNotFoundException("Agent not found by token"));
//    }



    /** Удаление агента */
    public void deleteAgent(Long agentId) {
        if (!agentRepo.existsById(agentId)) {
            throw new AgentNotFoundException("Agent not found: " + agentId);
        }
        agentRepo.deleteById(agentId);
    }

    /** Список агентов с пейджингом */
    @Transactional(readOnly = true)
    public Page<AgentDTO> listAgents(Pageable pageable) {
        return agentRepo.findAll(pageable).map(a -> toDto(a, false));
    }


    /** Преобразование entity -> dto. showToken = true когда нужно вернуть token (регистрация/ротация) */
    private AgentDTO toDto(Agent agent, boolean showToken) {
        AgentDTO dto = new AgentDTO();
        dto.setId(agent.getId());
        dto.setAgentName(agent.getAgentName());
        dto.setIpAddress(agent.getIpAddress());
        dto.setCountry(agent.getAgentCountry());
        dto.setLastHeartbeat(agent.getLastHeartbeat());
        dto.setStatus(agent.getStatus().name());
        if (showToken) {
            dto.setAgentToken(agent.getAgentToken());
        } else {
            dto.setAgentToken(null);
        }
        return dto;
    }

    /** Генерация токена (UUIDv4). При необходимости заменить на HMAC/JWT/сложную стратегию. */
    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    // --- Spring Security integration ---
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws org.springframework.security.core.userdetails.UsernameNotFoundException {
        return agentRepo.findByAgentName(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Agent not found: " + username));
    }
}
