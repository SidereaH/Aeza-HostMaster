package aeza.hostmaster.checks.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CheckStatus {
    OK,
    WARN,
    FAILED,
    UNKNOWN, FAIL;

    @JsonCreator
    public static CheckStatus from(String s) {
        if (s == null) return null;
        try {
            return CheckStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
