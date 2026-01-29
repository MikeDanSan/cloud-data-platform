package com.michael.backendservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final Counter jobsCreated;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter emrJobsStarted;
    private final Counter emrJobsFailed;
    private final Timer jobProcessingTime;

    public MetricsService(MeterRegistry registry) {
        this.jobsCreated = Counter.builder("jobs.created")
                .description("Total number of jobs created")
                .register(registry);

        this.jobsCompleted = Counter.builder("jobs.completed")
                .description("Total number of jobs completed successfully")
                .register(registry);

        this.jobsFailed = Counter.builder("jobs.failed")
                .description("Total number of jobs failed")
                .register(registry);

        this.emrJobsStarted = Counter.builder("emr.jobs.started")
                .description("Total number of EMR jobs started")
                .register(registry);

        this.emrJobsFailed = Counter.builder("emr.jobs.failed")
                .description("Total number of EMR jobs failed")
                .register(registry);

        this.jobProcessingTime = Timer.builder("jobs.processing.time")
                .description("Time taken for job to complete")
                .register(registry);
    }

    public void incrementJobsCreated() {
        jobsCreated.increment();
    }

    public void incrementJobsCompleted() {
        jobsCompleted.increment();
    }

    public void incrementJobsFailed() {
        jobsFailed.increment();
    }

    public void incrementEmrJobsStarted() {
        emrJobsStarted.increment();
    }

    public void incrementEmrJobsFailed() {
        emrJobsFailed.increment();
    }

    public void recordJobProcessingTime(long durationMillis) {
        jobProcessingTime.record(durationMillis, TimeUnit.MILLISECONDS);
    }
}