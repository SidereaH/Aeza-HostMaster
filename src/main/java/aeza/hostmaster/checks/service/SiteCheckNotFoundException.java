package aeza.hostmaster.checks.service;

public class SiteCheckNotFoundException extends RuntimeException {
    public SiteCheckNotFoundException(String jobId) {
        super("Site check job not found: " + jobId);
    }
}
