package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.dto.CheckJobResponse;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.dto.WebSocketMessage;
import aeza.hostmaster.checks.entity.SiteCheckEntity;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CheckJobServiceTest {

    @Mock
    private SiteCheckRepository siteCheckRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CheckJobService checkJobService;

    @Captor
    private ArgumentCaptor<SiteCheckEntity> siteCheckCaptor;

    @Captor
    private ArgumentCaptor<WebSocketMessage> wsMessageCaptor;

    private final UUID jobId = UUID.randomUUID();
    private final String target = "https://status.example.com";

    // ===== createJob =====

    @Test
    @DisplayName("createJob: сохраняет задачу в БД и отправляет WebSocket JOB_CREATED")
    void createJob_savesJobAndSendsWebSocket() {
        // when
        CheckJobResponse response = checkJobService.createJob(target);

        // then: проверяем сохранение в репозиторий
        verify(siteCheckRepository).save(siteCheckCaptor.capture());
        SiteCheckEntity saved = siteCheckCaptor.getValue();

        assertNotNull(saved.getId(), "id должен быть установлен");
        assertEquals(target, saved.getTarget());
        assertEquals(CheckStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getExecutedAt(), "executedAt должен быть установлен");
        assertNull(saved.getFinishedAt(), "finishedAt должен быть null при создании");
        assertNull(saved.getTotalDurationMillis(), "totalDurationMillis должен быть null при создании");

        // then: проверяем WebSocket-сообщение
        verify(messagingTemplate)
                .convertAndSend(eq("/topic/jobs/" + saved.getId()), wsMessageCaptor.capture());

        WebSocketMessage message = wsMessageCaptor.getValue();
        assertEquals("JOB_CREATED", message.type());
        assertEquals(saved.getId(), message.jobId());
        assertEquals(CheckStatus.PENDING, message.status());
        assertNotNull(message.timestamp());
        assertNull(message.data(), "data должно быть null для JOB_CREATED");

        // then: проверяем возвращаемый DTO
        assertEquals(saved.getId(), response.jobId());
        assertEquals(target, response.target());
        assertEquals(CheckStatus.PENDING, response.status());
        assertNotNull(response.executedAt());
        assertNull(response.finishedAt());
        assertNull(response.totalDurationMillis());
        assertNull(response.result());
    }

    // ===== updateJobStatus =====

    @Nested
    class UpdateJobStatusTests {

        @Test
        @DisplayName("updateJobStatus: обновляет статус на IN_PROGRESS без finishedAt")
        void updateJobStatus_inProgress() {
            SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.PENDING);
            when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

            checkJobService.updateJobStatus(jobId, CheckStatus.IN_PROGRESS);

            verify(siteCheckRepository).save(siteCheckCaptor.capture());
            SiteCheckEntity saved = siteCheckCaptor.getValue();

            assertEquals(CheckStatus.IN_PROGRESS, saved.getStatus());
            assertNull(saved.getFinishedAt(), "finishedAt не должен ставиться для IN_PROGRESS");

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());

            WebSocketMessage message = wsMessageCaptor.getValue();
            assertEquals("JOB_UPDATED", message.type());
            assertEquals(jobId, message.jobId());
            assertEquals(CheckStatus.IN_PROGRESS, message.status());
        }

        @Test
        @DisplayName("updateJobStatus: при COMPLETED выставляет finishedAt и отправляет WebSocket")
        void updateJobStatus_completed() {
            SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.IN_PROGRESS);
            when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

            checkJobService.updateJobStatus(jobId, CheckStatus.COMPLETED);

            verify(siteCheckRepository).save(siteCheckCaptor.capture());
            SiteCheckEntity saved = siteCheckCaptor.getValue();

            assertEquals(CheckStatus.COMPLETED, saved.getStatus());
            assertNotNull(saved.getFinishedAt(), "finishedAt должен быть выставлен для COMPLETED");

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());
            WebSocketMessage message = wsMessageCaptor.getValue();
            assertEquals("JOB_UPDATED", message.type());
            assertEquals(CheckStatus.COMPLETED, message.status());
        }

        @Test
        @DisplayName("updateJobStatus: при FAILED выставляет finishedAt и отправляет WebSocket")
        void updateJobStatus_failed() {
            SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.IN_PROGRESS);
            when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

            checkJobService.updateJobStatus(jobId, CheckStatus.FAILED);

            verify(siteCheckRepository).save(siteCheckCaptor.capture());
            SiteCheckEntity saved = siteCheckCaptor.getValue();

            assertEquals(CheckStatus.FAILED, saved.getStatus());
            assertNotNull(saved.getFinishedAt(), "finishedAt должен быть выставлен для FAILED");

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());
            WebSocketMessage message = wsMessageCaptor.getValue();
            assertEquals("JOB_UPDATED", message.type());
            assertEquals(CheckStatus.FAILED, message.status());
        }

        @Test
        @DisplayName("updateJobStatus: при TIMEOUT сейчас не выставляет finishedAt (поведение фиксируем тестом)")
        void updateJobStatus_timeoutCurrentBehavior() {
            SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.IN_PROGRESS);
            when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

            checkJobService.updateJobStatus(jobId, CheckStatus.TIMEOUT);

            verify(siteCheckRepository).save(siteCheckCaptor.capture());
            SiteCheckEntity saved = siteCheckCaptor.getValue();

            assertEquals(CheckStatus.TIMEOUT, saved.getStatus());
            // Текущее поведение сервиса: finishedAt не выставляется для TIMEOUT
            assertNull(saved.getFinishedAt(), "По текущей логике finishedAt для TIMEOUT остается null");

            verify(messagingTemplate)
                    .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());
            WebSocketMessage message = wsMessageCaptor.getValue();
            assertEquals("JOB_UPDATED", message.type());
            assertEquals(CheckStatus.TIMEOUT, message.status());
        }

        @Test
        @DisplayName("updateJobStatus: если job не найдена, ни репозиторий, ни WebSocket не дергаются")
        void updateJobStatus_jobNotFound() {
            when(siteCheckRepository.findById(jobId)).thenReturn(Optional.empty());

            checkJobService.updateJobStatus(jobId, CheckStatus.COMPLETED);

            verify(siteCheckRepository, never()).save(any());
            verifyNoInteractions(messagingTemplate);
        }
    }

    // ===== completeJob =====

    @Test
    @DisplayName("completeJob: завершает задачу, выставляет поля и отправляет JOB_COMPLETED с CheckJobResponse")
    void completeJob_success() {
        SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.IN_PROGRESS);
        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

        SiteCheckResponse result = mock(SiteCheckResponse.class);
        when(result.totalDurationMillis()).thenReturn(1234L);

        checkJobService.completeJob(jobId, result);

        // Проверка сохранения в БД
        verify(siteCheckRepository).save(siteCheckCaptor.capture());
        SiteCheckEntity saved = siteCheckCaptor.getValue();

        assertEquals(CheckStatus.COMPLETED, saved.getStatus());
        assertNotNull(saved.getFinishedAt(), "finishedAt должен быть выставлен при completeJob");
        assertEquals(1234L, saved.getTotalDurationMillis());

        // Проверка WebSocket
        verify(messagingTemplate)
                .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());

        WebSocketMessage message = wsMessageCaptor.getValue();
        assertEquals("JOB_COMPLETED", message.type());
        assertEquals(jobId, message.jobId());
        assertEquals(CheckStatus.COMPLETED, message.status());
        assertNotNull(message.timestamp());
        assertNotNull(message.data(), "data должно содержать CheckJobResponse");

        assertTrue(message.data() instanceof CheckJobResponse);
        CheckJobResponse wsResponse = (CheckJobResponse) message.data();

        assertEquals(jobId, wsResponse.jobId());
        assertEquals(target, wsResponse.target());
        assertEquals(CheckStatus.COMPLETED, wsResponse.status());
        assertEquals(entity.getExecutedAt(), wsResponse.executedAt());
        assertEquals(saved.getFinishedAt(), wsResponse.finishedAt());
        assertEquals(1234L, wsResponse.totalDurationMillis());
        assertSame(result, wsResponse.result(), "Внутри CheckJobResponse должен лежать переданный result");
    }

    @Test
    @DisplayName("completeJob: если job не найдена, ничего не делает")
    void completeJob_jobNotFound() {
        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.empty());
        SiteCheckResponse result = mock(SiteCheckResponse.class);

        checkJobService.completeJob(jobId, result);

        verify(siteCheckRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    // ===== appendJobLog =====

    @Test
    @DisplayName("appendJobLog: если job существует, отправляет JOB_LOG с текущим статусом")
    void appendJobLog_jobExists() {
        SiteCheckEntity entity = new SiteCheckEntity(jobId, target, Instant.now(), CheckStatus.IN_PROGRESS);
        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

        Object payload = "log line";

        checkJobService.appendJobLog(jobId, payload);

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());

        WebSocketMessage message = wsMessageCaptor.getValue();
        assertEquals("JOB_LOG", message.type());
        assertEquals(jobId, message.jobId());
        assertEquals(CheckStatus.IN_PROGRESS, message.status());
        assertEquals(payload, message.data());
    }

    @Test
    @DisplayName("appendJobLog: если job не найдена, отправляет JOB_LOG со status = null")
    void appendJobLog_jobNotFound() {
        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.empty());

        Object payload = "log line";

        checkJobService.appendJobLog(jobId, payload);

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/jobs/" + jobId), wsMessageCaptor.capture());

        WebSocketMessage message = wsMessageCaptor.getValue();
        assertEquals("JOB_LOG", message.type());
        assertEquals(jobId, message.jobId());
        assertNull(message.status(), "status должен быть null, если job не найдена");
        assertEquals(payload, message.data());
    }

    // ===== getJobStatus =====

    @Test
    @DisplayName("getJobStatus: мапит SiteCheckEntity в CheckJobResponse")
    void getJobStatus_success() {
        Instant executedAt = Instant.now().minusSeconds(10);
        Instant finishedAt = Instant.now();
        Long totalDuration = 1000L;

        SiteCheckEntity entity = new SiteCheckEntity(jobId, target, executedAt, CheckStatus.COMPLETED, totalDuration);
        entity.setFinishedAt(finishedAt);

        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.of(entity));

        CheckJobResponse response = checkJobService.getJobStatus(jobId);

        assertEquals(jobId, response.jobId());
        assertEquals(target, response.target());
        assertEquals(CheckStatus.COMPLETED, response.status());
        assertEquals(executedAt, response.executedAt());
        assertEquals(finishedAt, response.finishedAt());
        assertEquals(totalDuration, response.totalDurationMillis());
        assertNull(response.result(), "result должен быть null в getJobStatus");
    }

    @Test
    @DisplayName("getJobStatus: если job нет, кидает RuntimeException")
    void getJobStatus_jobNotFound() {
        when(siteCheckRepository.findById(jobId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> checkJobService.getJobStatus(jobId)
        );

        assertTrue(ex.getMessage().contains("Job not found"));
    }
}
