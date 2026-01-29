package com.michael.backendservice.jobs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michael.backendservice.observability.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@Tag(name = "Jobs API", description = "Public endpoints for job management")
public class JobsController {

    private final JobsRepository repo;
    private final S3Presigner presigner;
    private final S3OutputLister outputLister;
    private final String rawBucket;
    private final String processedBucket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MetricsService metricsService;

    public JobsController(
        JobsRepository repo,
        S3Presigner presigner,
        S3OutputLister outputLister,
        MetricsService metricsService,
        @Value("${app.s3.rawBucket}") String rawBucket,
        @Value("${app.s3.processedBucket}") String processedBucket
    ) {
        this.repo = repo;
        this.presigner = presigner;
        this.outputLister = outputLister;
        this.metricsService = metricsService;
        this.rawBucket = rawBucket;
        this.processedBucket = processedBucket;
    }

    @Operation(
            summary = "List jobs",
            description = "Returns paginated list of jobs. Use limit and lastKey (base64-encoded JSON) for pagination."
    )
    @GetMapping
    public ResponseEntity<?> listJobs(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "lastKey", required = false) String lastKey
    ) {
        Map<String, String> lastEvaluatedKey = null;

        if (lastKey != null && !lastKey.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(lastKey));
                lastEvaluatedKey = objectMapper.readValue(decoded, new TypeReference<>() {});
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid lastKey format"
                ));
            }
        }

        JobsPage page = repo.listJobs(limit, lastEvaluatedKey);

        String nextKey = null;
        if (page.lastEvaluatedKey() != null) {
            try {
                String json = objectMapper.writeValueAsString(page.lastEvaluatedKey());
                nextKey = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
            } catch (Exception e) {
                // Log error but don't fail the response
            }
        }

        return ResponseEntity.ok(Map.of(
                "items", page.items(),
                "lastEvaluatedKey", nextKey != null ? nextKey : ""
        ));
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
        metricsService.incrementJobsCreated();
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

    @Operation(
            summary = "Generate download URL",
            description = "Generates a presigned S3 GET URL for downloading processed job results. Only available for SUCCEEDED jobs."
    )
    @PostMapping("/{jobId}/download-url")
    public ResponseEntity<?> createDownloadUrl(@PathVariable String jobId) {
        Job job = repo.getJob(jobId).orElse(null);

        if (job == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Job not found",
                    "jobId", jobId
            ));
        }

        if (!JobStatus.SUCCEEDED.name().equals(job.status())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Job must be in SUCCEEDED status to download results",
                    "currentStatus", job.status()
            ));
        }

        if (job.outputS3Key() == null || job.outputS3Key().isBlank()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "No output available for this job",
                    "jobId", jobId
            ));
        }

        // For directory-based output (Parquet partitions), return URL to _SUCCESS marker
        String downloadKey = job.outputS3Key() + "_SUCCESS";
        int expires = 300;

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(processedBucket)
                .key(downloadKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

        return ResponseEntity.ok(new DownloadUrlResponse(
                jobId,
                processedBucket,
                downloadKey,
                presigned.url().toString(),
                expires
        ));
    }

    @Operation(
            summary = "List job output files",
            description = "Lists all output files from a completed job with presigned download URLs. Only available for SUCCEEDED jobs."
    )
    @GetMapping("/{jobId}/outputs")
    public ResponseEntity<?> listOutputFiles(@PathVariable String jobId) {
        Job job = repo.getJob(jobId).orElse(null);

        if (job == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Job not found",
                    "jobId", jobId
            ));
        }

        if (!JobStatus.SUCCEEDED.name().equals(job.status())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Job must be in SUCCEEDED status to view outputs",
                    "currentStatus", job.status()
            ));
        }

        if (job.outputS3Key() == null || job.outputS3Key().isBlank()) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "No output available for this job",
                    "jobId", jobId
            ));
        }

        List<OutputFile> files = outputLister.listOutputFiles(processedBucket, job.outputS3Key());

        long totalSize = files.stream().mapToLong(OutputFile::fileSizeBytes).sum();

        return ResponseEntity.ok(new OutputFilesResponse(
                jobId,
                job.status(),
                job.outputS3Key(),
                files,
                files.size(),
                totalSize
        ));
    }

}