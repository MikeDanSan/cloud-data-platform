package com.michael.backendservice.jobs;

import java.util.List;

public record JobsPage(
        List<Job> items,
        String lastEvaluatedKey
) {}