package com.michael.backendservice.jobs;

import java.util.List;

public record OutputFilesResponse(
        String jobId,
        String status,
        String outputPath,
        List<OutputFile> files,
        int totalFiles,
        long totalSizeBytes
) {}