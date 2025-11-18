package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.HttpCheckDetailsDto;
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
            if (check.metrics() != null && !check.metrics().isEmpty()) {
                check.metrics().forEach(metricDto -> {
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
                null,
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