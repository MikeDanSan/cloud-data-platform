package com.michael.backendservice.jobs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/jobs")
@Tag(name = "Internal API", description = "Internal endpoints for job workers (not for public use)")
public class InternalJobsController {

    private final JobsRepository repo;
    private final EmrService emrService;
    private final String rawBucket;
    private final String processedBucket;

    public InternalJobsController(
            JobsRepository repo,
            EmrService emrService,
            @Value("${app.s3.rawBucket:cloud-data-platform-dev-raw}") String rawBucket,
            @Value("${app.s3.processedBucket:cloud-data-platform-dev-processed}") String processedBucket
    ) {
        this.repo = repo;
        this.emrService = emrService;
        this.rawBucket = rawBucket;
        this.processedBucket = processedBucket;
    }

    @Operation(
            summary = "Update job status",
            description = "Updates job status with state machine validation. Valid transitions: SUBMITTED→RUNNING, RUNNING→SUCCEEDED|FAILED. Terminal states cannot transition."
    )
    @PostMapping("/{jobId}/status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status or transition"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<?> updateStatus(
            @PathVariable String jobId,
            @RequestBody UpdateJobStatusRequest request
    ) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing status"
            ));
        }

        JobStatus newStatus;
        try {
            newStatus = JobStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status",
                    "allowed", new String[]{"SUBMITTED", "RUNNING", "SUCCEEDED", "FAILED"}
            ));
        }

        Job job = repo.getJob(jobId).orElse(null);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Job not found",
                    "jobId", jobId
            ));
        }

        JobStatus currentStatus = JobStatus.valueOf(job.status());

        // Validate status transition
        if (!JobStatusTransitionValidator.isValidTransition(currentStatus, newStatus)) {
            var allowedStatuses = JobStatusTransitionValidator.getAllowedTransitions(currentStatus)
                    .stream()
                    .map(JobStatus::name)
                    .collect(Collectors.toSet());

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status transition",
                    "currentStatus", currentStatus.name(),
                    "attemptedStatus", newStatus.name(),
                    "allowedNextStatuses", allowedStatuses
            ));
        }

        // Update status first
        Job updated = repo.updateStatus(jobId, newStatus, request.message(), request.outputS3Key());

        // If transitioning to RUNNING, start EMR job
        if (newStatus == JobStatus.RUNNING && job.inputS3Key() != null) {
            try {
                String inputPath = "s3://" + rawBucket + "/" + job.inputS3Key();
                String outputPath = "s3://" + processedBucket + "/processed/jobs/" + jobId + "/output/";

                String emrJobRunId = emrService.startSparkJob(jobId, inputPath, outputPath);
                if (emrJobRunId != null && !emrJobRunId.isBlank()) {
                    repo.setEmrJobRunId(jobId, emrJobRunId);
                    // Fetch updated job with EMR ID
                    updated = repo.getJob(jobId).orElse(updated);
                } else {
                    // EMR disabled or not configured; leave job RUNNING without emrJobRunId
                    System.err.println("EMR disabled or not configured; jobRunId not set for jobId=" + jobId);
                }
            } catch (Exception e) {
                // Log error but don't fail the status update
                System.err.println("Failed to start EMR job: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(updated);
    }
}