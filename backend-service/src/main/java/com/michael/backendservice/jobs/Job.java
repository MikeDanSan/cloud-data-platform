package com.michael.backendservice.jobs;

public record Job(
        String jobId,
        String status,
        String createdAt,
        String inputS3Key,
        String updatedAt,
        String statusMessage,
        String outputS3Key,
        String emrJobRunId
) {}