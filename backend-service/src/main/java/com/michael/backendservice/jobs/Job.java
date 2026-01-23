package com.michael.backendservice.jobs;

public record Job(
        String jobId,
        String status,
        String createdAt
) {}
