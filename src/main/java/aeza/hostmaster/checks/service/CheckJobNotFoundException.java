package aeza.hostmaster.checks.service;

import java.util.UUID;

/**
 * Indicates that a requested check job does not exist.
 */
public class CheckJobNotFoundException extends RuntimeException {

    public CheckJobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId);
    }
}
