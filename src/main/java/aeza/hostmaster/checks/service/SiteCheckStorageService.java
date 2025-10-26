package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.entity.*;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SiteCheckStorageService {

    private final SiteCheckRepository siteCheckRepository;

    public SiteCheckStorageService(SiteCheckRepository siteCheckRepository) {
        this.siteCheckRepository = siteCheckRepository;
    }

    @Transactional
    public void saveSiteCheck(SiteCheckResponse response) {
        SiteCheckEntity siteCheck = new SiteCheckEntity(
                response.id(),
                response.target(),
                response.executedAt(),
                response.status(),
                response.totalDurationMillis()
        );

        if (response.checks() != null) {
            response.checks().forEach(check -> {
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
        }
        siteCheckRepository.save(siteCheck);
    }
}