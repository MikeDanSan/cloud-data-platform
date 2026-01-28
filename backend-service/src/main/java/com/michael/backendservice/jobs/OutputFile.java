package com.michael.backendservice.jobs;

public record OutputFile(
        String fileName,
        String s3Key,
        long fileSizeBytes,
        String downloadUrl,
        int expiresInSeconds
) {}