package aeza.hostmaster.checks.dto;

import aeza.hostmaster.checks.domain.CheckType;

import java.util.List;
import java.util.Map;

public record SiteCheckCreateRequest(
        String target,
        List<CheckType> checkTypes,
        Map<String, Object> parameters,
        Integer timeoutSeconds
) {}
