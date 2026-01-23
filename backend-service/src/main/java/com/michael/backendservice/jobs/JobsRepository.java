package com.michael.backendservice.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class JobsRepository {

    private final DynamoDbClient ddb;
    private final String jobsTable;

    public JobsRepository(DynamoDbClient ddb,
                          @Value("${app.dynamodb.jobsTable}") String jobsTable) {
        this.ddb = ddb;
        this.jobsTable = jobsTable;
    }

    public Job createJob(String jobId) {
        Instant now = Instant.now();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("jobId", AttributeValue.fromS(jobId));
        item.put("status", AttributeValue.fromS(JobStatus.SUBMITTED.name()));
        item.put("createdAt", AttributeValue.fromS(now.toString()));

        ddb.putItem(PutItemRequest.builder()
                .tableName(jobsTable)
                .item(item)
                .build());

        return new Job(jobId, JobStatus.SUBMITTED.name(), now.toString());
    }

    public Optional<Job> getJob(String jobId) {
        Map<String, AttributeValue> key = Map.of("jobId", AttributeValue.fromS(jobId));

        var resp = ddb.getItem(GetItemRequest.builder()
                .tableName(jobsTable)
                .key(key)
                .consistentRead(true)
                .build());

        if (!resp.hasItem() || resp.item().isEmpty()) {
            return Optional.empty();
        }

        Map<String, AttributeValue> item = resp.item();
        String status = item.getOrDefault("status", AttributeValue.fromS("UNKNOWN")).s();
        String createdAt = item.getOrDefault("createdAt", AttributeValue.fromS("")).s();

        return Optional.of(new Job(jobId, status, createdAt));
    }
}
