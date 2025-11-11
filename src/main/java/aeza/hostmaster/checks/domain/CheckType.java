package aeza.hostmaster.checks.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CheckType {
    HTTP,       // HTTP(S) GET проверка
    PING,       // Ping проверка
    TCP,        // TCP connect проверка (используйте TCP вместо TCP_CONNECT)
    TRACEROUTE, // Traceroute с геопозицией
    TCP_CONNECT, DNS_LOOKUP;  // DNS lookup проверка

    @JsonCreator
    public static CheckType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "HTTP", "HTTP_GET" -> HTTP;
            case "PING", "ICMP" -> PING;
            case "TCP" -> TCP;
            case "TCP_CONNECT", "TCP-CONNECT", "TCP_CONNECTIVITY" -> TCP_CONNECT;
            case "DNS", "DNS_LOOKUP", "DNS-LOOKUP" -> DNS_LOOKUP;
            case "TRACEROUTE", "TRACE_ROUTE", "TRACE" -> TRACEROUTE;
            default -> {
                try {
                    yield CheckType.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}