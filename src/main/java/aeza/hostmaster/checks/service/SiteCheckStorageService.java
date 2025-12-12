package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.*;
import aeza.hostmaster.checks.entity.*;
import aeza.hostmaster.checks.repository.CheckExecutionRepository;
import aeza.hostmaster.checks.repository.CheckLogRepository;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SiteCheckStorageService {

    private final SiteCheckRepository siteRepo;
    private final CheckExecutionRepository execRepo;
    private final CheckLogRepository logRepo;
    private final PayloadNormalizer normalizer;
    private final ObjectMapper mapper;
    public SiteCheckStorageService(SiteCheckRepository siteRepo,
                                   CheckExecutionRepository execRepo,
                                   CheckLogRepository logRepo,
                                   PayloadNormalizer normalizer, ObjectMapper mapper) {
        this.siteRepo = siteRepo;
        this.execRepo = execRepo;
        this.logRepo = logRepo;
        this.normalizer = normalizer;
        this.mapper = mapper;
    }


    public SiteCheckEntity getJob(UUID id) {
        return siteRepo.findById(id).orElse(null);
    }



    // ---------------------- SAVE EXECUTION -------------------------

    @Transactional
    public CheckExecutionEntity saveExecution(SiteCheckEntity job, JsonNode root) {

        JsonNode payload = root.path("payload");

        CheckExecutionEntity e = new CheckExecutionEntity();
        e.setSiteCheck(job);

        // тип чека определяем по payload (ping/http/...)
        CheckType type = normalizer.detectType(payload);
        e.setType(type);

        // статус агента: success/failed/error/completed/...
        String agentStatus = root.path("status").asText("").toLowerCase();
        CheckStatus execStatus;
        if ("success".equals(agentStatus) || "completed".equals(agentStatus)) {
            execStatus = CheckStatus.COMPLETED;
        } else if ("failed".equals(agentStatus) || "error".equals(agentStatus)) {
            execStatus = CheckStatus.FAILED;
        } else {
            execStatus = CheckStatus.IN_PROGRESS;
        }
        e.setStatus(execStatus);

        e.setDurationMillis(root.path("duration").asLong());
        e.setTimestamp(Instant.now());

        if (root.has("error")) {
            String error = root.get("error").asText(null);
            if (error != null && !error.isBlank()) {
                e.setMessage(error);
            }
        }

        if (!payload.isMissingNode() && !payload.isNull()) {
            e.setRawPayloadJson(payload.toString());
        }

        return execRepo.save(e);
    }



    // ---------------------- SAVE LOG ------------------------

    @Transactional
    public CheckLogEntity saveLog(SiteCheckEntity job, String json) {
        CheckLogEntity log = new CheckLogEntity();
        log.setSiteCheck(job);
        log.setTimestamp(Instant.now());
        log.setRawJson(json);
        return logRepo.save(log);
    }


    // ---------------------- BUILD DTO FOR WS ------------------------

    public CheckExecutionResponse buildExecutionDto(CheckExecutionEntity exec, JsonNode raw) {

        JsonNode payload = raw.path("payload");
        CheckType type = exec.getType();

        HttpDetailsDto http = null;
        PingDetailsDto ping = null;
        TcpDetailsDto tcp = null;
        TracerouteDetailsDto traceroute = null;
        DnsDetailsDto dns = null;

        switch (type) {
            case HTTP -> {
                JsonNode httpNode = payload.path("http");
                if (!httpNode.isMissingNode() && !httpNode.isNull()) {
                    http = normalizer.normalizeHttp(httpNode);
                }
            }
            case PING -> {
                JsonNode pingNode = payload.path("ping");
                if (!pingNode.isMissingNode() && !pingNode.isNull()) {
                    // агент шлёт массив, нормализатор должен уметь работать с массивом
                    ping = normalizer.normalizePing(pingNode);
                }
            }
            case TCP, TCP_CONNECT -> {
                JsonNode tcpNode = payload.path("tcp");
                if (!tcpNode.isMissingNode() && !tcpNode.isNull()) {
                    tcp = normalizer.normalizeTcp(tcpNode);
                }
            }
            case TRACEROUTE -> {
                JsonNode trNode = payload.path("traceroute");
                if (!trNode.isMissingNode() && !trNode.isNull()) {
                    traceroute = normalizer.normalizeTraceroute(trNode);
                }
            }
            case DNS_LOOKUP -> {
                JsonNode dnsNode = payload.path("dns");
                if (!dnsNode.isMissingNode() && !dnsNode.isNull()) {
                    dns = normalizer.normalizeDns(dnsNode);
                }
            }
        }

        return new CheckExecutionResponse(
                exec.getId(),
                type,
                exec.getStatus(),        // уже мапится из agent.status
                exec.getDurationMillis(),
                exec.getMessage(),
                http,
                ping,
                tcp,
                traceroute,
                dns
        );
    }

    public List<CheckExecutionResponse> buildExecutionDtosForJob(UUID jobId) {
        List<CheckExecutionEntity> executions = execRepo.findBySiteCheckId(jobId);

        List<CheckExecutionResponse> result = new ArrayList<>();

        for (CheckExecutionEntity exec : executions) {
            JsonNode payloadNode;
            try {
                if (exec.getRawPayloadJson() != null) {
                    payloadNode = mapper.readTree(exec.getRawPayloadJson());
                } else {
                    payloadNode = mapper.createObjectNode();
                }
            } catch (Exception e) {
                payloadNode = mapper.createObjectNode();
            }

            // buildExecutionDto ожидает root с полем payload
            ObjectNode root = mapper.createObjectNode();
            root.set("payload", payloadNode);

            CheckExecutionResponse dto = buildExecutionDto(exec, root);
            result.add(dto);
        }

        return result;
    }

}
