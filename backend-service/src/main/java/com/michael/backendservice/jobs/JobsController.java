package com.michael.backendservice.jobs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobsController {

    private final JobsRepository repo;

    public JobsController(JobsRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createJob() {
        String jobId = UUID.randomUUID().toString();
        repo.createJob(jobId);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        return repo.getJob(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "error", "Job not found",
                        "jobId", jobId
                )));
    }
}
