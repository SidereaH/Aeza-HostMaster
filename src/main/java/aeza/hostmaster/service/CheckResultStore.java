package aeza.hostmaster.service;

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

    private final ConcurrentMap<UUID, String> results = new ConcurrentHashMap<>();

    public void store(UUID checkId, String payload) {
        results.put(checkId, payload);
    }

    public Optional<String> find(UUID checkId) {
        return Optional.ofNullable(results.get(checkId));
    }
}
