package aeza.hostmaster.checks.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Supported DNS record types captured during lookup checks.
 */
public enum DnsRecordType {
    A,
    AAAA,
    MX,
    NS,
    TXT,
    CNAME, // Canonical name
    SOA;    // Start of authority

    @JsonCreator
    public static DnsRecordType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        try {
            return DnsRecordType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
