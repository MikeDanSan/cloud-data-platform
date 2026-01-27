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

    public String startSparkJob(String jobId, String inputS3Path, String outputS3Path) {
        log.info("Starting Spark job for jobId={}, input={}, output={}", jobId, inputS3Path, outputS3Path);

        StartJobRunRequest request = StartJobRunRequest.builder()
                .applicationId(applicationId)
                .executionRoleArn(jobRoleArn)
                .name("job-" + jobId)
                .jobDriver(JobDriver.builder()
                        .sparkSubmit(SparkSubmit.builder()
                                .entryPoint(sparkJarPath)
                                .entryPointArguments(inputS3Path, outputS3Path)
                                .sparkSubmitParameters(
                                        "--class com.michael.spark.SimpleTransformJob " +
                                        "--conf spark.executor.cores=4 " +
                                        "--conf spark.executor.memory=8g " +
                                        "--conf spark.driver.cores=2 " +
                                        "--conf spark.driver.memory=4g"
                                )
                                .build())
                        .build())
                .configurationOverrides(ConfigurationOverrides.builder()
                        .monitoringConfiguration(MonitoringConfiguration.builder()
                                .cloudWatchLoggingConfiguration(CloudWatchLoggingConfiguration.builder()
                                        .enabled(true)
                                        .logGroupName("/aws/emr-serverless/jobs")
                                        .build())
                                .build())
                        .build())
                .tags(Map.of("JobId", jobId))
                .build();

        StartJobRunResponse response = emrClient.startJobRun(request);
        String jobRunId = response.jobRunId();

        log.info("Started EMR job run: jobRunId={} for jobId={}", jobRunId, jobId);
        return jobRunId;
    }
}