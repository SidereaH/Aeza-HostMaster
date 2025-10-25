package aeza.hostmaster.checks.domain;

public enum CheckType {
    HTTP,       // HTTP(S) GET проверка
    PING,       // Ping проверка
    TCP,        // TCP connect проверка (используйте TCP вместо TCP_CONNECT)
    TRACEROUTE, // Traceroute с геопозицией
    TCP_CONNECT, DNS_LOOKUP  // DNS lookup проверка
}