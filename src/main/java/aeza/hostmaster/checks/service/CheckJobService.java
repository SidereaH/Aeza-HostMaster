package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckStatus;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.entity.SiteCheckEntity;
import aeza.hostmaster.checks.repository.SiteCheckRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CheckJobService {

    private final SiteCheckRepository siteRepo;
    private final SimpMessagingTemplate ws;

    public CheckJobService(SiteCheckRepository siteRepo, SimpMessagingTemplate ws) {
        this.siteRepo = siteRepo;
        this.ws = ws;
    }


    // create job (called from REST controller)
    public SiteCheckEntity createJob(String target) {
        SiteCheckEntity job = new SiteCheckEntity(
                UUID.randomUUID(),
                target,
                Instant.now(),
                CheckStatus.PENDING
        );
        return siteRepo.save(job);
    }


    public void updateStatus(SiteCheckEntity job, CheckStatus status) {
        job.setStatus(status);
        siteRepo.save(job);

        ws.convertAndSend("/topic/jobs/" + job.getId(), status);
    }
    public SiteCheckEntity findJob(UUID id) {
        return siteRepo.findById(id).orElseThrow(() -> new SiteCheckNotFoundException(id.toString()));
    }


    public void completeJob(SiteCheckEntity job, SiteCheckResponse resp) {
        job.setStatus(CheckStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        job.setTotalDurationMillis(resp.totalDurationMillis());
        siteRepo.save(job);

        ws.convertAndSend("/topic/jobs/" + job.getId(), resp);
    }


    public SiteCheckResponse toResponse(SiteCheckEntity job) {
        return new SiteCheckResponse(
                job.getId(),
                job.getTarget(),
                job.getExecutedAt(),
                job.getStatus(),
                job.getTotalDurationMillis(),
                null
        );
    }
}
