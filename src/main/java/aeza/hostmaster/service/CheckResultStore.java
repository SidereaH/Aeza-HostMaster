package aeza.hostmaster.service;

import aeza.hostmaster.checks.dto.SiteCheckResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * Thread-safe in-memory storage for results that arrive from Kafka.
 */
@Component
public class CheckResultStore {

    private final ConcurrentMap<UUID, SiteCheckResponse> results = new ConcurrentHashMap<>();

    public void store(UUID checkId, SiteCheckResponse payload) {
        results.put(checkId, payload);
    }

    public Optional<SiteCheckResponse> find(UUID checkId) {
        return Optional.ofNullable(results.get(checkId));
    }
}
