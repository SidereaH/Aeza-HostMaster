package aeza.hostmaster.checks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.domain.DnsRecordType;
import aeza.hostmaster.checks.dto.CheckExecutionRequest;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.DnsLookupDetailsDto;
import aeza.hostmaster.checks.dto.DnsRecordDto;
import aeza.hostmaster.checks.dto.HttpCheckDetailsDto;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.dto.TcpCheckDetailsDto;
import aeza.hostmaster.checks.dto.TracerouteDetailsDto;
import aeza.hostmaster.checks.dto.TracerouteHopDto;
import aeza.hostmaster.checks.repository.SiteCheckResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SiteCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SiteCheckResultRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateAndReturnCheckResult() throws Exception {
        SiteCheckRequest request = buildRequest("https://example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/check-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.checks", hasSize(5)))
                .andReturn();

        SiteCheckResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), SiteCheckResponse.class);

        mockMvc.perform(get("/api/v1/check-results/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value("https://example.com"))
                .andExpect(jsonPath("$.checks", hasSize(5)))
                .andExpect(jsonPath("$.checks[0].metrics", hasSize(1)));
    }

    @Test
    void shouldFilterCheckResultsByTarget() throws Exception {
        SiteCheckRequest request1 = buildRequest("https://example.com");
        SiteCheckRequest request2 = buildRequest("https://another.com");

        mockMvc.perform(post("/api/v1/check-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/check-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        MvcResult filtered = mockMvc.perform(get("/api/v1/check-results")
                        .param("target", "another"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andReturn();

        JsonNode node = objectMapper.readTree(filtered.getResponse().getContentAsString());
        assertThat(node.get("content").get(0).get("target").asText()).isEqualTo("https://another.com");
    }

    @Test
    void shouldReturnNotFoundForMissingCheck() throws Exception {
        mockMvc.perform(get("/api/v1/check-results/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCheckWithMissingDetails() throws Exception {
        CheckExecutionRequest httpCheck = new CheckExecutionRequest(
                CheckType.HTTP,
                CheckStatus.SUCCESS,
                120L,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
        SiteCheckRequest request = new SiteCheckRequest(
                "https://invalid.com",
                Instant.parse("2024-01-01T00:00:00Z"),
                CheckStatus.FAILURE,
                120L,
                List.of(httpCheck));

        mockMvc.perform(post("/api/v1/check-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private SiteCheckRequest buildRequest(String target) {
        Instant executedAt = Instant.parse("2024-01-01T00:00:00Z");
        CheckExecutionRequest httpCheck = new CheckExecutionRequest(
                CheckType.HTTP,
                CheckStatus.SUCCESS,
                150L,
                "OK",
                new HttpCheckDetailsDto("GET", 200, 120L, Map.of("Content-Type", "text/html")),
                null,
                null,
                null,
                null,
                List.of(new CheckMetricDto("throughput", 123.4, "KB/s", "Measured transfer rate")));

        CheckExecutionRequest pingCheck = new CheckExecutionRequest(
                CheckType.PING,
                CheckStatus.SUCCESS,
                40L,
                null,
                null,
                new PingCheckDetailsDto(4, 4, 0.0, 20L, 25L, 30L, 2L),
                null,
                null,
                null,
                List.of());

        CheckExecutionRequest tcpCheck = new CheckExecutionRequest(
                CheckType.TCP_CONNECT,
                CheckStatus.SUCCESS,
                30L,
                null,
                null,
                null,
                new TcpCheckDetailsDto(443, 15L, "93.184.216.34"),
                null,
                null,
                List.of());

        CheckExecutionRequest tracerouteCheck = new CheckExecutionRequest(
                CheckType.TRACEROUTE,
                CheckStatus.SUCCESS,
                500L,
                null,
                null,
                null,
                null,
                new TracerouteDetailsDto(List.of(
                        new TracerouteHopDto(1, "192.168.0.1", "router", 10L, 55.7558, 37.6173, "RU", "Moscow"),
                        new TracerouteHopDto(2, "93.184.216.34", "example.com", 40L, 37.751, -97.822, "US", "Los Angeles"))),
                null,
                List.of());

        CheckExecutionRequest dnsCheck = new CheckExecutionRequest(
                CheckType.DNS_LOOKUP,
                CheckStatus.SUCCESS,
                60L,
                null,
                null,
                null,
                null,
                null,
                new DnsLookupDetailsDto(List.of(
                        new DnsRecordDto(DnsRecordType.A, "93.184.216.34", 3600L),
                        new DnsRecordDto(DnsRecordType.MX, "mail.example.com", 7200L))),
                List.of());

        List<CheckExecutionRequest> checks = List.of(httpCheck, pingCheck, tcpCheck, tracerouteCheck, dnsCheck);

        return new SiteCheckRequest(target, executedAt, CheckStatus.SUCCESS, 780L, checks);
    }
}
