package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.HttpCheckDetailsDto;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.entity.*;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SiteCheckStorageService {

    private final SiteCheckRepository siteCheckRepository;

    public SiteCheckStorageService(SiteCheckRepository siteCheckRepository) {
        this.siteCheckRepository = siteCheckRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SiteCheckResponse> findSiteCheck(UUID id) {
        return siteCheckRepository.findById(id).map(this::mapToResponse);
    }

    @Transactional
    public void saveSiteCheck(SiteCheckResponse response) {
        SiteCheckEntity existing = siteCheckRepository.findById(response.id()).orElse(null);

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

        SiteCheckEntity siteCheck = new SiteCheckEntity(
                response.id(),
                target,
                executedAt,
                status,
                response.totalDurationMillis()
        );

        // Сохраняем checks
        List<CheckExecutionResponse> checks = response.checks() == null
                ? List.of()
                : response.checks();

        checks.forEach(check -> {
            CheckExecutionEntity checkEntity = new CheckExecutionEntity(
                    check.id(),
                    check.type(),
                    check.status(),
                    check.durationMillis(),
                    check.message()
            );

            // Сохраняем HTTP details если есть
            if (check.httpDetails() != null) {
                HttpDetailsEntity httpDetails = new HttpDetailsEntity();
                httpDetails.setId(UUID.randomUUID());
                httpDetails.setMethod(check.httpDetails().method());
                httpDetails.setStatusCode(check.httpDetails().statusCode());
                httpDetails.setResponseTimeMillis(check.httpDetails().responseTimeMillis());
                httpDetails.setHeaders(check.httpDetails().headers());
                checkEntity.setHttpDetails(httpDetails);
            }

            // Сохраняем metrics если есть
            List<CheckMetricDto> metrics = mergeWithPingMetrics(check);
            if (!metrics.isEmpty()) {
                metrics.forEach(metricDto -> {
                    CheckMetricEntity metric = new CheckMetricEntity();
                    metric.setId(UUID.randomUUID());
                    metric.setName(metricDto.name());
                    metric.setValue(metricDto.value());
                    metric.setUnit(metricDto.unit());
                    metric.setDescription(metricDto.description());
                    checkEntity.addMetric(metric);
                });
            }

            siteCheck.addCheck(checkEntity);
        });

        siteCheckRepository.save(siteCheck);
    }

    private SiteCheckResponse mapToResponse(SiteCheckEntity entity) {
        List<CheckExecutionResponse> checks = entity.getChecks().stream()
                .map(this::mapExecution)
                .collect(Collectors.toList());

        return new SiteCheckResponse(
                entity.getId(),
                entity.getTarget(),
                entity.getExecutedAt(),
                entity.getStatus(),
                entity.getTotalDurationMillis(),
                checks
        );
    }

    private CheckExecutionResponse mapExecution(CheckExecutionEntity entity) {
        return new CheckExecutionResponse(
                entity.getId(),
                entity.getType(),
                entity.getStatus(),
                entity.getDurationMillis(),
                entity.getMessage(),
                mapHttp(entity),
                extractPing(entity.getMetrics()),
                null,
                null,
                null,
                entity.getMetrics().stream()
                        .map(metric -> new CheckMetricDto(
                                metric.getName(),
                                metric.getValue(),
                                metric.getUnit(),
                                metric.getDescription()
                        ))
                        .collect(Collectors.toList())
        );
    }

    private List<CheckMetricDto> mergeWithPingMetrics(CheckExecutionResponse check) {
        List<CheckMetricDto> metrics = check.metrics() == null ? List.of() : check.metrics();
        if (check.pingDetails() == null) {
            return metrics;
        }

        PingCheckDetailsDto ping = check.pingDetails();
        List<CheckMetricDto> merged = metrics.stream().collect(Collectors.toList());

        merged.add(new CheckMetricDto("ping.packets.transmitted", asDouble(ping.packetsTransmitted()), null, null));
        merged.add(new CheckMetricDto("ping.packets.received", asDouble(ping.packetsReceived()), null, null));
        merged.add(new CheckMetricDto("ping.packets.loss", ping.packetLossPercentage(), "%", null));
        merged.add(new CheckMetricDto("ping.rtt.min", ping.minimumRttMillis(), "ms", null));
        merged.add(new CheckMetricDto("ping.rtt.avg", ping.averageRttMillis(), "ms", null));
        merged.add(new CheckMetricDto("ping.rtt.max", ping.maximumRttMillis(), "ms", null));
        merged.add(new CheckMetricDto("ping.rtt.stddev", ping.standardDeviationRttMillis(), "ms", null));

        return merged.stream().filter(metric -> metric.value() != null).collect(Collectors.toList());
    }

    private PingCheckDetailsDto extractPing(List<CheckMetricEntity> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        Double transmitted = findMetricValue(metrics, "ping.packets.transmitted");
        Double received = findMetricValue(metrics, "ping.packets.received");
        Double loss = findMetricValue(metrics, "ping.packets.loss");
        Double min = findMetricValue(metrics, "ping.rtt.min");
        Double avg = findMetricValue(metrics, "ping.rtt.avg");
        Double max = findMetricValue(metrics, "ping.rtt.max");
        Double stddev = findMetricValue(metrics, "ping.rtt.stddev");

        if (transmitted == null && received == null && loss == null && min == null && avg == null && max == null && stddev == null) {
            return null;
        }

        return new PingCheckDetailsDto(
                transmitted != null ? transmitted.intValue() : null,
                received != null ? received.intValue() : null,
                loss,
                min,
                avg,
                max,
                stddev
        );
    }

    private Double findMetricValue(List<CheckMetricEntity> metrics, String name) {
        return metrics.stream()
                .filter(metric -> name.equals(metric.getName()))
                .map(CheckMetricEntity::getValue)
                .findFirst()
                .orElse(null);
    }

    private Double asDouble(Integer value) {
        return value == null ? null : value.doubleValue();
    }

    private HttpCheckDetailsDto mapHttp(CheckExecutionEntity entity) {
        if (!CheckType.HTTP.equals(entity.getType()) || entity.getHttpDetails() == null) {
            return null;
        }

        HttpDetailsEntity details = entity.getHttpDetails();
        return new HttpCheckDetailsDto(
                details.getMethod(),
                details.getStatusCode(),
                details.getResponseTimeMillis(),
                details.getHeaders()
        );
    }
}