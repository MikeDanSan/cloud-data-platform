package com.michael.backendservice.jobs;

public record DownloadUrlResponse(
        String jobId,
        String bucket,
        String s3Key,
        String downloadUrl,
        int expiresInSeconds
) {}