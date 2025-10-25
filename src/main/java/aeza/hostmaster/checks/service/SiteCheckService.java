package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SiteCheckService {

    private final WebClient webClient;

    public SiteCheckService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("")
                .build();
    }

    public SiteCheckResponse performSiteCheck(SiteCheckCreateRequest request) {
        Instant startTime = Instant.now();

        try {
            // Выполняем реальный HTTP запрос
            var httpResult = performHttpCheck(request.target());
            Instant endTime = Instant.now();
            long totalDuration = Duration.between(startTime, endTime).toMillis();

            // Создаем ответ с реальными данными проверки
            return new SiteCheckResponse(
                    UUID.randomUUID(),
                    request.target(),
                    Instant.now(),
                    httpResult.status(),
                    totalDuration,
                    List.of(httpResult)
            );

        } catch (Exception e) {
            Instant endTime = Instant.now();
            long totalDuration = Duration.between(startTime, endTime).toMillis();

            // Создаем ответ с ошибкой
            return new SiteCheckResponse(
                    UUID.randomUUID(),
                    request.target(),
                    Instant.now(),
                    CheckStatus.FAIL,
                    totalDuration,
                    List.of(createErrorCheckExecution(e.getMessage(), totalDuration))
            );
        }
    }

    private CheckExecutionResponse performHttpCheck(String target) {
        Instant startTime = Instant.now();

        try {
            var response = webClient.get()
                    .uri(target)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(30));

            Instant endTime = Instant.now();
            long duration = Duration.between(startTime, endTime).toMillis();

            HttpStatus status = (HttpStatus) response.getStatusCode();
            boolean isSuccess = status.is2xxSuccessful() || status.is3xxRedirection();

            return new CheckExecutionResponse(
                    UUID.randomUUID(),
                    CheckType.HTTP,
                    isSuccess ? CheckStatus.OK : CheckStatus.FAIL,
                    duration,
                    status.value() + " " + status.getReasonPhrase(),
                    new HttpCheckDetailsDto(
                            "GET",
                            status.value(),
                            duration,
                            response.getHeaders().toSingleValueMap()
                    ),
                    null, // pingDetails
                    null, // tcpDetails
                    null, // tracerouteDetails
                    null, // dnsLookupDetails
                    List.of() // metrics
            );

        } catch (Exception e) {
            Instant endTime = Instant.now();
            long duration = Duration.between(startTime, endTime).toMillis();

            return new CheckExecutionResponse(
                    UUID.randomUUID(),
                    CheckType.HTTP,
                    CheckStatus.FAIL,
                    duration,
                    "Error: " + e.getMessage(),
                    new HttpCheckDetailsDto(
                            "GET",
                            0,
                            duration,
                            java.util.Map.of()
                    ),
                    null, null, null, null, List.of()
            );
        }
    }

    private CheckExecutionResponse createErrorCheckExecution(String errorMessage, long duration) {
        return new CheckExecutionResponse(
                UUID.randomUUID(),
                CheckType.HTTP,
                CheckStatus.FAIL,
                duration,
                errorMessage,
                new HttpCheckDetailsDto(
                        "GET",
                        0,
                        duration,
                        java.util.Map.of()
                ),
                null, null, null, null, List.of()
        );
    }
}