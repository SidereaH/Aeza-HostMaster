package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.dto.*;

import aeza.hostmaster.checks.entity.SiteCheckEntity;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CheckJobService {

    private final SiteCheckRepository siteCheckRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public CheckJobService(SiteCheckRepository siteCheckRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.siteCheckRepository = siteCheckRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public CheckJobResponse createJob(String target) {
        UUID jobId = UUID.randomUUID();
        Instant executedAt = Instant.now();

        // Сохраняем в БД со статусом PENDING
        SiteCheckEntity job = new SiteCheckEntity(jobId, target, executedAt, CheckStatus.PENDING);
        siteCheckRepository.save(job);

        // Отправляем WebSocket сообщение
        sendWebSocketMessage(jobId, "JOB_CREATED", CheckStatus.PENDING, null);

        return new CheckJobResponse(
                jobId, target, CheckStatus.PENDING, executedAt, null, null, null
        );
    }

    @Transactional
    public void updateJobStatus(UUID jobId, CheckStatus status) {
        siteCheckRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            if (status == CheckStatus.COMPLETED || status == CheckStatus.FAILED) {
                job.setFinishedAt(Instant.now());
            }
            siteCheckRepository.save(job);

            sendWebSocketMessage(jobId, "JOB_UPDATED", status, null);
        });
    }

    @Transactional
    public void completeJob(UUID jobId, SiteCheckResponse result) {
        siteCheckRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(CheckStatus.COMPLETED);
            job.setFinishedAt(Instant.now());
            job.setTotalDurationMillis(result.totalDurationMillis());
            siteCheckRepository.save(job);

            CheckJobResponse response = new CheckJobResponse(
                    jobId, job.getTarget(), CheckStatus.COMPLETED,
                    job.getExecutedAt(), job.getFinishedAt(),
                    result.totalDurationMillis(), result
            );

            sendWebSocketMessage(jobId, "JOB_COMPLETED", CheckStatus.COMPLETED, response);
        });
    }

    @Transactional(readOnly = true)
    public void appendJobLog(UUID jobId, Object payload) {
        CheckStatus status = siteCheckRepository.findById(jobId)
                .map(SiteCheckEntity::getStatus)
                .orElse(null);

        sendWebSocketMessage(jobId, "JOB_LOG", status, payload);
    }

    private void sendWebSocketMessage(UUID jobId, String type, CheckStatus status, Object data) {
        WebSocketMessage message = new WebSocketMessage(
                type, jobId, status, Instant.now(), data
        );

        messagingTemplate.convertAndSend("/topic/jobs/" + jobId, message);
    }

    public CheckJobResponse getJobStatus(UUID jobId) {
        return siteCheckRepository.findById(jobId)
                .map(job -> new CheckJobResponse(
                        job.getId(), job.getTarget(), job.getStatus(),
                        job.getExecutedAt(), job.getFinishedAt(),
                        job.getTotalDurationMillis(), null
                ))
                .orElseThrow(() -> new CheckJobNotFoundException(jobId));
    }
}