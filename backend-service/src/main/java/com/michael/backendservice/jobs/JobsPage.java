package com.michael.backendservice.jobs;

import java.util.List;
import java.util.Map;

public record JobsPage(
        List<Job> items,
        Map<String, String> lastEvaluatedKey // Changed from String to Map
) {}