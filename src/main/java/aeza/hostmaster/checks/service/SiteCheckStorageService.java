package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.HttpCheckDetailsDto;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.domain.CheckExecutionResult;
import aeza.hostmaster.checks.domain.CheckMetric;
import aeza.hostmaster.checks.domain.SiteCheckResult;
import aeza.hostmaster.checks.repository.SiteCheckResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SiteCheckStorageService {

    private final SiteCheckResultRepository siteCheckResultRepository;
    private final SiteCheckMapper siteCheckMapper;

    public SiteCheckStorageService(SiteCheckResultRepository siteCheckResultRepository,
                                   SiteCheckMapper siteCheckMapper) {
        this.siteCheckResultRepository = siteCheckResultRepository;
        this.siteCheckMapper = siteCheckMapper;
    }

    @Transactional(readOnly = true)
    public Optional<SiteCheckResponse> findSiteCheck(UUID id) {
        return siteCheckResultRepository.findById(id).map(siteCheckMapper::toResponse);
    }

    @Transactional
    public void saveSiteCheck(SiteCheckResponse response) {
        SiteCheckResult existing = siteCheckResultRepository.findById(response.id()).orElse(null);

        String target = response.target();
        if ((target == null || target.isBlank()) && existing != null) {
            target = existing.getTarget();
        }
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Site check response target is required");
        }

        Instant executedAt = response.executedAt();
        if (executedAt == null && existing != null) {
            executedAt = existing.getExecutedAt();
        }
        if (executedAt == null) {
            executedAt = Instant.now();
        }

        CheckStatus status = response.status();
        if (status == null && existing != null) {
            status = existing.getStatus();
        }
        if (status == null) {
            status = CheckStatus.COMPLETED;
        }

        SiteCheckResult siteCheck = existing != null ? existing : new SiteCheckResult();
        siteCheck.setId(response.id());
        siteCheck.setTarget(target);
        siteCheck.setExecutedAt(executedAt);
        siteCheck.setStatus(status);
        siteCheck.setTotalDurationMillis(response.totalDurationMillis());

        if (existing != null) {
            siteCheck.getChecks().clear();
        }

        // Сохраняем checks
        List<CheckExecutionResponse> checks = response.checks() == null
                ? List.of()
                : response.checks();

        checks.forEach(check -> {
            CheckExecutionResult checkEntity = new CheckExecutionResult();
            checkEntity.setType(check.type());
            checkEntity.setStatus(check.status());
            checkEntity.setDurationMillis(check.durationMillis());
            checkEntity.setMessage(check.message());

            // Сохраняем HTTP details если есть
            if (check.httpDetails() != null) {
                checkEntity.setHttpDetails(siteCheckMapper.toHttpEntity(check.httpDetails()));
            }

            if (check.pingDetails() != null) {
                checkEntity.setPingDetails(siteCheckMapper.toPingEntity(check.pingDetails()));
            }

            if (check.tcpDetails() != null) {
                checkEntity.setTcpDetails(siteCheckMapper.toTcpEntity(check.tcpDetails()));
            }

            if (check.tracerouteDetails() != null) {
                checkEntity.setTracerouteDetails(siteCheckMapper.toTracerouteEntity(check.tracerouteDetails()));
            }

            if (check.dnsLookupDetails() != null) {
                checkEntity.setDnsLookupDetails(siteCheckMapper.toDnsLookupEntity(check.dnsLookupDetails()));
            }

            List<CheckMetricDto> metrics = mergeWithPingMetrics(check);
            if (!metrics.isEmpty()) {
                List<CheckMetric> metricEntities = metrics.stream()
                        .map(siteCheckMapper::toMetricEntity)
                        .toList();
                checkEntity.setMetrics(metricEntities);
            }

            siteCheck.addCheck(checkEntity);
        });

        siteCheckResultRepository.save(siteCheck);
    }

    private List<CheckMetricDto> mergeWithPingMetrics(CheckExecutionResponse check) {
        LinkedHashMap<String, CheckMetricDto> merged = new LinkedHashMap<>();

        if (check.metrics() != null) {
            check.metrics().stream()
                    .filter(Objects::nonNull)
                    .forEach(metric -> merged.put(metric.name(), metric));
        }

        if (CheckType.PING.equals(check.type()) && check.pingDetails() != null) {
            PingCheckDetailsDto pingDetails = check.pingDetails();
            addMetric(merged, "packets_transmitted", pingDetails.packetsTransmitted(), null, "Ping packets transmitted");
            addMetric(merged, "packets_received", pingDetails.packetsReceived(), null, "Ping packets received");
            addMetric(merged, "packet_loss_pct", pingDetails.packetLossPercentage(), "%", "Ping packet loss percentage");
            addMetric(merged, "rtt_min_ms", pingDetails.minimumRttMillis(), "ms", "Ping minimum round-trip time");
            addMetric(merged, "rtt_avg_ms", pingDetails.averageRttMillis(), "ms", "Ping average round-trip time");
            addMetric(merged, "rtt_max_ms", pingDetails.maximumRttMillis(), "ms", "Ping maximum round-trip time");
            addMetric(merged, "rtt_stddev_ms", pingDetails.standardDeviationRttMillis(), "ms", "Ping RTT standard deviation");
        }

        return merged.values().stream().toList();
    }

    private void addMetric(LinkedHashMap<String, CheckMetricDto> metrics,
                           String name,
                           Number value,
                           String unit,
                           String description) {
        if (value == null) {
            return;
        }

        metrics.putIfAbsent(name, new CheckMetricDto(name, value.doubleValue(), unit, description));
    }

}