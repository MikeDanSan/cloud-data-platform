package com.michael.backendservice.jobs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@Tag(name = "Jobs API", description = "Public endpoints for job management")
public class JobsController {

    private final JobsRepository repo;
    private final S3Presigner presigner;
    private final String rawBucket;

    public JobsController(
            JobsRepository repo,
            S3Presigner presigner,
            @Value("${app.s3.rawBucket}") String rawBucket
    ) {
        this.repo = repo;
        this.presigner = presigner;
        this.rawBucket = rawBucket;
    }

    @Operation(
            summary = "List jobs",
            description = "Returns paginated list of jobs. Use limit and lastKey for pagination."
    )
    @GetMapping
    public ResponseEntity<?> listJobs(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "lastKey", required = false) String lastKey
    ) {
        JobsPage page = repo.listJobs(limit, lastKey);
        return ResponseEntity.ok(page);
    }

    @Operation(
            summary = "Create a job",
            description = "Creates a new job record with SUBMITTED status. Optionally accepts inputS3Key."
    )
    @PostMapping
    public ResponseEntity<?> createJob(@RequestBody(required = false) CreateJobRequest request) {
        String jobId = UUID.randomUUID().toString();
        String inputS3Key = (request == null) ? null : request.inputS3Key();
        Job created = repo.createJob(jobId, inputS3Key);
        return ResponseEntity.ok(created);
    }

    @Operation(
            summary = "Get job",
            description = "Retrieves job details by jobId including status, timestamps, and S3 keys."
    )
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        return repo.getJob(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "error", "Job not found",
                        "jobId", jobId
                )));
    }

    @Operation(
            summary = "Generate upload URL",
            description = "Generates a presigned S3 PUT URL for uploading job input data and persists the S3 key to the job record."
    )
    @PostMapping("/{jobId}/upload-url")
    public ResponseEntity<?> createUploadUrl(@PathVariable String jobId) {
        if (repo.getJob(jobId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Job not found",
                    "jobId", jobId
            ));
        }

        String s3Key = "raw/jobs/" + jobId + "/input.csv";
        repo.setInputS3Key(jobId, s3Key);

        int expires = 300;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(rawBucket)
                .key(s3Key)
                .contentType("text/csv")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        return ResponseEntity.ok(new UploadUrlResponse(
                jobId,
                rawBucket,
                s3Key,
                presigned.url().toString(),
                expires
        ));
    }
}