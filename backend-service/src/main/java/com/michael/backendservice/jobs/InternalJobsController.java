package com.michael.backendservice.jobs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        if (repo.getJob(jobId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Job not found",
                    "jobId", jobId
            ));
        }

        Job updated = repo.updateStatus(jobId, newStatus, request.message());
        return ResponseEntity.ok(updated);
    }
}
