package aeza.hostmaster.agents.services;


import aeza.hostmaster.agents.dto.AgentDTO;
import aeza.hostmaster.agents.dto.AgentRegistrationRequest;
import aeza.hostmaster.agents.exceptions.AgentAlreadyExistsException;
import aeza.hostmaster.agents.exceptions.AgentNotFoundException;
import aeza.hostmaster.agents.models.Agent;
import aeza.hostmaster.agents.repositories.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository repo;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AgentService service;

    @Captor
    private ArgumentCaptor<Agent> agentCaptor;

    private Agent sampleAgent;

    @BeforeEach
    void setUp() {
        sampleAgent = new Agent();
        sampleAgent.setId(1L);
        sampleAgent.setAgentName("agent-1");
        sampleAgent.setIpAddress("10.0.0.1");
        sampleAgent.setAgentCountry("EE");
        sampleAgent.setAgentToken("$2a$10$hashplaceholder"); // hashed
        sampleAgent.setCreatedAt(OffsetDateTime.now().minusDays(1));
    }

    @Test
    void registerAgent_Success() {
        AgentRegistrationRequest req = new AgentRegistrationRequest("new-agent", "1.2.3.4", "FI");
        when(repo.existsByAgentName("new-agent")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("hashedToken");
        Agent saved = new Agent();
        saved.setId(10L);
        saved.setAgentName("new-agent");
        saved.setAgentToken("hashedToken");
        when(repo.save(any(Agent.class))).thenReturn(saved);

        AgentDTO dto = service.registerAgent(req);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getAgentName()).isEqualTo("new-agent");
        assertThat(dto.getAgentToken()).isNotNull(); // raw token returned
        verify(repo).save(agentCaptor.capture());
        Agent passed = agentCaptor.getValue();
        assertThat(passed.getAgentToken()).isEqualTo("hashedToken");
    }

    @Test
    void registerAgent_NameExists_Throws() {
        when(repo.existsByAgentName("x")).thenReturn(true);
        AgentRegistrationRequest req = new AgentRegistrationRequest("x","1.1.1.1","US");
        assertThatThrownBy(() -> service.registerAgent(req)).isInstanceOf(AgentAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void rotateToken_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(sampleAgent));
        when(encoder.encode(anyString())).thenReturn("newHash");
        when(repo.save(any())).thenAnswer(i -> {
            Agent a = (Agent) i.getArgument(0);
            a.setId(1L);
            return a;
        });

        AgentDTO dto = service.rotateToken(1L);
        assertThat(dto.getAgentToken()).isNotNull();
        verify(repo).save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getAgentToken()).isEqualTo("newHash");
    }

    @Test
    void rotateToken_NotFound_Throws() {
        when(repo.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotateToken(2L)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void updateHeartbeat_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(sampleAgent));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        AgentDTO dto = service.updateHeartbeat(1L);
        assertThat(dto.getLastHeartbeat()).isNotNull();
        verify(repo).save(any());
    }

    @Test
    void updateHeartbeat_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateHeartbeat(99L)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void validateToken_SuccessAndFail() {
        when(repo.findByAgentName("agent-1")).thenReturn(Optional.of(sampleAgent));
        when(encoder.matches("raw", "$2a$10$hashplaceholder")).thenReturn(true);
        assertThat(service.validateToken("agent-1","raw")).isTrue();

        when(repo.findByAgentName("unknown")).thenReturn(Optional.empty());
        assertThat(service.validateToken("unknown","raw")).isFalse();
    }

    @Test
    void heartbeatByNameAndToken_Success() {
        when(repo.findByAgentName("agent-1")).thenReturn(Optional.of(sampleAgent));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        AgentDTO dto = service.heartbeatByNameAndToken("agent-1","raw");
        assertThat(dto.getLastHeartbeat()).isNotNull();
    }

    @Test
    void heartbeatByNameAndToken_BadCredentials() {
        when(repo.findByAgentName("agent-1")).thenReturn(Optional.of(sampleAgent));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.heartbeatByNameAndToken("agent-1","bad"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void heartbeatByNameAndToken_AgentNotFound() {
        when(repo.findByAgentName("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.heartbeatByNameAndToken("no","raw")).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void getAgent_SuccessAndNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(sampleAgent));
        AgentDTO dto = service.getAgent(1L);
        assertThat(dto.getAgentName()).isEqualTo("agent-1");

        when(repo.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAgent(2L)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void getAgentByName_SuccessAndNotFound() {
        when(repo.findByAgentName("agent-1")).thenReturn(Optional.of(sampleAgent));
        AgentDTO dto = service.getAgentByName("agent-1");
        assertThat(dto.getAgentName()).isEqualTo("agent-1");

        when(repo.findByAgentName("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAgentByName("no")).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void deleteAgent_SuccessAndNotFound() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);
        service.deleteAgent(1L);
        verify(repo).deleteById(1L);

        when(repo.existsById(2L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteAgent(2L)).isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void listAgents() {
        when(repo.findAll(PageRequest.of(0,10))).thenReturn(new PageImpl<>(List.of(sampleAgent)));
        var page = service.listAgents(PageRequest.of(0,10));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void loadUserByUsername_SuccessAndNotFound() {
        when(repo.findByAgentName("agent-1")).thenReturn(Optional.of(sampleAgent));
        var ud = service.loadUserByUsername("agent-1");
        assertThat(ud.getUsername()).isEqualTo("agent-1");

        when(repo.findByAgentName("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("no")).isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}