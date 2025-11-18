package aeza.hostmaster.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import aeza.hostmaster.service.CheckResultStore;

@WebMvcTest(CheckResultController.class)
class CheckResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckResultStore store;

    @Test
    void shouldReturnResultWhenPresent() throws Exception {
        UUID checkId = UUID.randomUUID();
        String payload = "{\"status\":\"ok\"}";

        when(store.find(checkId)).thenReturn(Optional.of(payload));

        mockMvc.perform(get("/api/checks/{id}/result", checkId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkId").value(checkId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payload").value(payload));
    }

    @Test
    void shouldReturnAcceptedWhenResultIsNotReady() throws Exception {
        UUID checkId = UUID.randomUUID();

        when(store.find(checkId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/checks/{id}/result", checkId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.checkId").value(checkId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payload").doesNotExist());
    }
}
