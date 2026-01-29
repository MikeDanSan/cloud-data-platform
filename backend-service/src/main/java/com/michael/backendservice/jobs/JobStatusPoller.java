package com.michael.backendservice.jobs;

import com.michael.backendservice.observability.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.emrserverless.model.JobRunState;

import java.time.Instant;
import java.util.List;

@Component
public class JobStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(JobStatusPoller.class);

    private final JobsRepository repo;
    private final EmrService emrService;
    private final MetricsService metricsService;
    private final String processedBucket;

    public JobStatusPoller(
            JobsRepository repo,
            EmrService emrService,
            MetricsService metricsService,
            @Value("${app.s3.processedBucket:cloud-data-platform-dev-processed}") String processedBucket
    ) {
        this.repo = repo;
        this.emrService = emrService;
        this.metricsService = metricsService;
        this.processedBucket = processedBucket;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void pollRunningJobs() {
        log.debug("Polling RUNNING jobs for EMR status updates");

        List<Job> runningJobs = repo.getJobsByStatus(JobStatus.RUNNING);
        
        for (Job job : runningJobs) {
            if (job.emrJobRunId() == null || job.emrJobRunId().isBlank()) {
                log.warn("Job {} is RUNNING but has no emrJobRunId; skipping", job.jobId());
                continue;
            }

            try {
                JobRunState emrState = emrService.getJobRunState(job.emrJobRunId());
                
                if (emrState == null) {
                    log.warn("Could not fetch EMR state for job {} (emrJobRunId={})", 
                            job.jobId(), job.emrJobRunId());
                    continue;
                }

                log.info("Job {} EMR state: {}", job.jobId(), emrState);

                switch (emrState) {
                    case SUCCESS:
                        String outputPath = "processed/jobs/" + job.jobId() + "/output/";
                        repo.updateStatus(
                                job.jobId(),
                                JobStatus.SUCCEEDED,
                                "EMR job completed successfully",
                                outputPath
                        );
                        metricsService.incrementJobsCompleted();
                        
                        // Calculate processing time
                        long processingTime = Instant.now().toEpochMilli() - 
                                Instant.parse(job.createdAt()).toEpochMilli();
                        metricsService.recordJobProcessingTime(processingTime);
                        
                        log.info("Job {} marked SUCCEEDED with output: {}", job.jobId(), outputPath);
                        break;

                    case FAILED:
                    case CANCELLED:
                        repo.updateStatus(
                                job.jobId(),
                                JobStatus.FAILED,
                                "EMR job " + emrState.toString().toLowerCase(),
                                null
                        );
                        metricsService.incrementJobsFailed();
                        metricsService.incrementEmrJobsFailed();
                        log.info("Job {} marked FAILED (EMR state: {})", job.jobId(), emrState);
                        break;

                    case PENDING:
                    case SCHEDULED:
                    case RUNNING:
                        log.debug("Job {} still in progress (EMR state: {})", job.jobId(), emrState);
                        break;

                    default:
                        log.warn("Unexpected EMR state {} for job {}", emrState, job.jobId());
                }
            } catch (Exception e) {
                log.error("Error polling EMR status for job {}: {}", job.jobId(), e.getMessage());
            }
        }
    }
}