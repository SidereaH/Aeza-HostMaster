package aeza.hostmaster.checks.domain;

/**
 * Represents the outcome of a single monitoring check or an aggregated run.
 */
public enum CheckStatus {
    SUCCESS,
    FAILURE,
    PARTIAL,
    UNKNOWN
}
