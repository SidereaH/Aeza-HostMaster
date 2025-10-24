package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.dto.CheckExecutionRequest;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.SiteCheckRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.repository.SiteCheckResultRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteCheckService {

    private final SiteCheckResultRepository repository;
    private final SiteCheckMapper mapper;

    public SiteCheckService(SiteCheckResultRepository repository, SiteCheckMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public SiteCheckResponse create(SiteCheckRequest request) {
        validateRequest(request);
        var entity = mapper.toEntity(request);
        var saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SiteCheckResponse get(UUID id) {
        var entity = repository.findById(id).orElseThrow(() -> new SiteCheckNotFoundException(id));
        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<SiteCheckResponse> find(String target, Pageable pageable) {
        var page = (target == null || target.isBlank())
                ? repository.findAll(pageable)
                : repository.findByTargetContainingIgnoreCase(target, pageable);
        return page.map(mapper::toResponse);
    }

    private void validateRequest(SiteCheckRequest request) {
        if (request == null) {
            throw new InvalidCheckDetailsException("Request payload is required");
        }
        if (request.target() == null || request.target().isBlank()) {
            throw new InvalidCheckDetailsException("Target must not be blank");
        }
        if (request.executedAt() == null) {
            throw new InvalidCheckDetailsException("Execution timestamp is required");
        }
        if (request.status() == null) {
            throw new InvalidCheckDetailsException("Overall status is required");
        }
        if (request.totalDurationMillis() == null || request.totalDurationMillis() < 0) {
            throw new InvalidCheckDetailsException("Total duration must be zero or positive");
        }
        if (request.checks() == null || request.checks().isEmpty()) {
            throw new InvalidCheckDetailsException("At least one check result must be provided");
        }
        for (int i = 0; i < request.checks().size(); i++) {
            CheckExecutionRequest check = request.checks().get(i);
            if (check == null) {
                throw new InvalidCheckDetailsException("Check entry %d is missing".formatted(i));
            }
            if (check.type() == null) {
                throw new InvalidCheckDetailsException("Check type is required for entry %d".formatted(i));
            }
            if (check.status() == null) {
                throw new InvalidCheckDetailsException("Check status is required for entry %d".formatted(i));
            }
            if (check.durationMillis() == null || check.durationMillis() < 0) {
                throw new InvalidCheckDetailsException("Check duration must be zero or positive for entry %d".formatted(i));
            }
            if (check.metrics() != null) {
                for (CheckMetricDto metric : check.metrics()) {
                    if (metric == null || metric.name() == null || metric.name().isBlank()) {
                        throw new InvalidCheckDetailsException("Metric name is required for check entry %d".formatted(i));
                    }
                }
            }
        }
    }
}
