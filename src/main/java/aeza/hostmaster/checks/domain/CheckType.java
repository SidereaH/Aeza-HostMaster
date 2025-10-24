package aeza.hostmaster.checks.domain;

/**
 * The type of monitoring check being executed for a site.
 */
public enum CheckType {
    HTTP,
    PING,
    TCP_CONNECT,
    TRACEROUTE,
    DNS_LOOKUP
}
