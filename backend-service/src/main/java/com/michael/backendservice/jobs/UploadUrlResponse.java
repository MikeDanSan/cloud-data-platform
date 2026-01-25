package com.michael.backendservice.jobs;

public record UploadUrlResponse(
        String jobId,
        String bucket,
        String s3Key,
        String uploadUrl,
        int expiresInSeconds
) {}
