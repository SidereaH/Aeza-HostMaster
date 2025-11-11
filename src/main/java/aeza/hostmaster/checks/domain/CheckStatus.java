package aeza.hostmaster.checks.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CheckStatus {
    PENDING,      // Ожидает выполнения
    IN_PROGRESS,  // Выполняется агентом
    COMPLETED,    // Успешно завершено
    FAILED,       // Завершено с ошибкой
    TIMEOUT,      // Таймаут
    OK,           // Для HTTP проверок - успех
    FAIL;         // Для HTTP проверок - ошибка

    @JsonCreator
    public static CheckStatus fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "SUCCESS", "SUCCEEDED" -> OK;
            case "OK" -> OK;
            case "FAIL", "FAILED", "ERROR" -> FAILED;
            case "TIMEOUT", "TIMED_OUT" -> TIMEOUT;
            case "IN_PROGRESS", "RUNNING", "INPROGRESS" -> IN_PROGRESS;
            case "PENDING", "QUEUED" -> PENDING;
            case "DONE", "COMPLETE", "COMPLETED" -> COMPLETED;
            default -> {
                try {
                    yield CheckStatus.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}