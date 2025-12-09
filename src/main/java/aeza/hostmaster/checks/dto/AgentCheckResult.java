package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record AgentCheckResult(
        String taskId,     // UUID как строка
        String agentId,    // идентификатор агента
        String status,     // "success", "failed", "error" и т.п.
        Long duration,     // длительность проверки в мс
        String error,      // текст ошибки, если есть
        String timestamp,  // ISO-строка времени
        JsonNode payload   // "сырой" payload агента (http/dns/ping/tcp/traceroute)
) {}
