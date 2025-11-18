package aeza.hostmaster.checks.service;

/**
 * Indicates that a site check could not be scheduled for processing, usually because
 * the messaging infrastructure is unavailable or rejected the task message.
 */
public class SiteCheckSchedulingException extends RuntimeException {

    public SiteCheckSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
