package aeza.hostmaster.checks.domain;

public enum CheckStatus {
    PENDING,      // Ожидает выполнения
    IN_PROGRESS,  // Выполняется агентом
    COMPLETED,    // Успешно завершено
    FAILED,       // Завершено с ошибкой
    TIMEOUT,      // Таймаут
    OK,           // Для HTTP проверок - успех
    FAIL          // Для HTTP проверок - ошибка
}