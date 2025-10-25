package aeza.hostmaster.agents.controllers;

import aeza.hostmaster.agents.controller.AgentController;
import aeza.hostmaster.agents.dto.AgentDTO;
import aeza.hostmaster.agents.dto.AgentRegistrationRequest;
import aeza.hostmaster.agents.services.AgentService;
import aeza.hostmaster.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgentController.class)
@Import(SecurityConfig.class) //
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentService agentService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void register_ReturnsCreated() throws Exception {
        AgentRegistrationRequest req = new AgentRegistrationRequest("a","1.1.1.1","RU");
        AgentDTO resp = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.registerAgent(any())).thenReturn(resp);

        mockMvc.perform(post("/api/agents/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentName", is("a")))
                .andExpect(jsonPath("$.agentToken", is("rawtoken")));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void heartbeatById_ReturnsOk() throws Exception {
        AgentDTO dto = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.updateHeartbeat(1L)).thenReturn(dto);
        mockMvc.perform(post("/api/agents/1/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void heartbeatByName_ReturnsOk() throws Exception {
        AgentDTO dto = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.heartbeatByNameAndToken(eq("a"), eq("raw"))).thenReturn(dto);
        mockMvc.perform(post("/api/agents/heartbeat")
                        .param("agentName","a")
                        .param("token","raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentName", is("a")));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void getById_ReturnsOk() throws Exception {
        AgentDTO dto = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.getAgent(1L)).thenReturn(dto);
        mockMvc.perform(get("/api/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentName", is("a")));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void list_ReturnsPage() throws Exception {
        AgentDTO dto = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.listAgents(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0,10), 1));
        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].agentName", is("a")));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void rotateToken_ReturnsNewToken() throws Exception {
        AgentDTO dto = new AgentDTO(1L,"a","1.1.1.1","RU","rawtoken", null, "ACTIVE");
        Mockito.when(agentService.rotateToken(1L)).thenReturn(dto);
        mockMvc.perform(post("/api/agents/1/rotate-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentToken", is("newtoken")));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void delete_ReturnsNoContent() throws Exception {
        Mockito.doNothing().when(agentService).deleteAgent(1L);
        mockMvc.perform(delete("/api/agents/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void validate_ReturnsBoolean() throws Exception {
        Mockito.when(agentService.validateToken("a","raw")).thenReturn(true);
        mockMvc.perform(get("/api/agents/validate")
                        .param("agentName","a")
                        .param("token","raw"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
