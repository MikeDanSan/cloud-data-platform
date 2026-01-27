package com.michael.backendservice.jobs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/jobs")
@Tag(name = "Internal API", description = "Internal endpoints for job workers (not for public use)")
public class InternalJobsController {

    private final JobsRepository repo;

    public InternalJobsController(JobsRepository repo) {
        this.repo = repo;
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

        Job updated = repo.updateStatus(jobId, newStatus, request.message(), request.outputS3Key());
        return ResponseEntity.ok(updated);
    }
}