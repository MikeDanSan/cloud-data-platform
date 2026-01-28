package com.michael.backendservice.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.emrserverless.EmrServerlessClient;
import software.amazon.awssdk.services.emrserverless.model.*;

import java.util.Map;

@Service
public class EmrService {

    private static final Logger log = LoggerFactory.getLogger(EmrService.class);

    private final EmrServerlessClient emrClient;
    private final String applicationId;
    private final String jobRoleArn;
    private final String sparkJarPath;

    public EmrService(
            EmrServerlessClient emrClient,
            @Value("${app.emr.applicationId}") String applicationId,
            @Value("${app.emr.jobRoleArn}") String jobRoleArn,
            @Value("${app.emr.sparkJarPath}") String sparkJarPath
    ) {
        this.emrClient = emrClient;
        this.applicationId = applicationId;
        this.jobRoleArn = jobRoleArn;
        this.sparkJarPath = sparkJarPath;
    }

    private boolean isConfigured() {
        return applicationId != null && !applicationId.isBlank()
                && jobRoleArn != null && !jobRoleArn.isBlank()
                && sparkJarPath != null && !sparkJarPath.isBlank();
    }

    public String startSparkJob(String jobId, String inputS3Path, String outputS3Path) {
        if (!isConfigured()) {
            log.warn("EMR not configured; skipping Spark submission for jobId={}", jobId);
            return null;
        }

        log.info("Starting Spark job for jobId={}, input={}, output={}", jobId, inputS3Path, outputS3Path);

        StartJobRunRequest request = StartJobRunRequest.builder()
                .applicationId(applicationId)
                .executionRoleArn(jobRoleArn)
                .jobDriver(JobDriver.builder()
                        .sparkSubmit(SparkSubmit.builder()
                                .entryPoint(sparkJarPath)
                                .entryPointArguments(inputS3Path, outputS3Path)
                                .sparkSubmitParameters("--class com.michael.spark.SimpleTransformJob")
                                .build())
                        .build())
                .configurationOverrides(ConfigurationOverrides.builder()
                        .monitoringConfiguration(MonitoringConfiguration.builder()
                                .s3MonitoringConfiguration(S3MonitoringConfiguration.builder()
                                        .logUri("s3://cloud-data-platform-dev-raw/emr-logs/")
                                        .build())
                                .build())
                        .build())
                .name("job-" + jobId)
                .tags(Map.of("JobId", jobId))
                .build();

        StartJobRunResponse response = emrClient.startJobRun(request);
        String jobRunId = response.jobRunId();

        log.info("Started EMR job run: jobRunId={} for jobId={}", jobRunId, jobId);
        return jobRunId;
    }

    public JobRunState getJobRunState(String jobRunId) {
        if (!isConfigured()) {
            log.warn("EMR not configured; cannot poll jobRunId={}", jobRunId);
            return null;
        }

        try {
            GetJobRunRequest request = GetJobRunRequest.builder()
                    .applicationId(applicationId)
                    .jobRunId(jobRunId)
                    .build();

            GetJobRunResponse response = emrClient.getJobRun(request);
            JobRun jobRun = response.jobRun();

            log.debug("EMR jobRunId={} state={}", jobRunId, jobRun.state());
            return jobRun.state();
        } catch (Exception e) {
            log.error("Failed to get EMR job run state for jobRunId={}: {}", jobRunId, e.getMessage());
            return null;
        }
    }
}