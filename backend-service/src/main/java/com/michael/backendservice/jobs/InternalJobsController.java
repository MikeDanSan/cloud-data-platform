package com.michael.backendservice.jobs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/jobs")
public class InternalJobsController {

    private final JobsRepository repo;

    public InternalJobsController(JobsRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/{jobId}/status")
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

        Job updated = repo.updateStatus(jobId, newStatus, request.message());
        return ResponseEntity.ok(updated);
    }
}