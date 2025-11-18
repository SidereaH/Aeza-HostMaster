package aeza.hostmaster.checks.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.service.KafkaSiteCheckService;
import aeza.hostmaster.checks.service.SiteCheckStorageService;
import aeza.hostmaster.service.CheckResultStore;

@WebMvcTest(SiteCheckController.class)
class SiteCheckControllerResultTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaSiteCheckService kafkaSiteCheckService;

    @MockBean
    private SiteCheckStorageService siteCheckStorageService;

    @MockBean
    private CheckResultStore checkResultStore;

    @Test
    void returnsInMemoryResultWhenPresent() throws Exception {
        UUID checkId = UUID.randomUUID();
        SiteCheckResponse response = new SiteCheckResponse(checkId, "example.com", Instant.EPOCH,
                CheckStatus.COMPLETED, 123L, List.of());

        when(checkResultStore.find(checkId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/checks/{id}/result", checkId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checkId.toString()))
                .andExpect(jsonPath("$.target").value("example.com"))
                .andExpect(jsonPath("$.status").value(CheckStatus.COMPLETED.name()));
    }

    @Test
    void returnsStoredResultWhenDatabaseHasEntry() throws Exception {
        UUID checkId = UUID.randomUUID();
        SiteCheckResponse response = new SiteCheckResponse(checkId, "example.com", Instant.EPOCH,
                CheckStatus.COMPLETED, 123L, List.of());

        when(checkResultStore.find(checkId)).thenReturn(Optional.empty());
        when(siteCheckStorageService.findSiteCheck(checkId)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/checks/{id}/result", checkId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checkId.toString()))
                .andExpect(jsonPath("$.status").value(CheckStatus.COMPLETED.name()));
    }

    @Test
    void returnsAcceptedWhenResultMissing() throws Exception {
        UUID checkId = UUID.randomUUID();

        when(checkResultStore.find(checkId)).thenReturn(Optional.empty());
        when(siteCheckStorageService.findSiteCheck(checkId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/checks/{id}/result", checkId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }
}
