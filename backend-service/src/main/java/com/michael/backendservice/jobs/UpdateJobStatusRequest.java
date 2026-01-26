package com.michael.backendservice.jobs;

public record UpdateJobStatusRequest(
        String status,
        String message
) {}