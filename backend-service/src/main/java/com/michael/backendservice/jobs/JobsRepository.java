package com.michael.backendservice.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

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

    public Job createJob(String jobId, String inputS3Key) {
        Instant now = Instant.now();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("jobId", AttributeValue.fromS(jobId));
        item.put("status", AttributeValue.fromS(JobStatus.SUBMITTED.name()));
        item.put("createdAt", AttributeValue.fromS(now.toString()));
        item.put("updatedAt", AttributeValue.fromS(now.toString()));

        if (inputS3Key != null && !inputS3Key.isEmpty()) {
            item.put("inputS3Key", AttributeValue.fromS(inputS3Key));
        }

        ddb.putItem(PutItemRequest.builder()
                .tableName(jobsTable)
                .item(item)
                .build());

        return new Job(jobId, JobStatus.SUBMITTED.name(), now.toString(), inputS3Key, now.toString(), null);
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
        String inputS3Key = item.containsKey("inputS3Key") ? item.get("inputS3Key").s() : null;
        String updatedAt = item.containsKey("updatedAt") ? item.get("updatedAt").s() : null;
        String statusMessage = item.containsKey("statusMessage") ? item.get("statusMessage").s() : null;

        return Optional.of(new Job(jobId, status, createdAt, inputS3Key, updatedAt, statusMessage));
    }

    public void setInputS3Key(String jobId, String inputS3Key) {
        Map<String, AttributeValue> key = Map.of("jobId", AttributeValue.fromS(jobId));

        Map<String, String> names = Map.of("#input", "inputS3Key");
        Map<String, AttributeValue> values = Map.of(":val", AttributeValue.fromS(inputS3Key));

        ddb.updateItem(UpdateItemRequest.builder()
                .tableName(jobsTable)
                .key(key)
                .updateExpression("SET #input = :val")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build());
    }

    public Job updateStatus(String jobId, JobStatus status, String message) {
        String now = Instant.now().toString();

        Map<String, AttributeValue> key = Map.of("jobId", AttributeValue.fromS(jobId));

        Map<String, String> names = Map.of(
                "#status", "status",
                "#updatedAt", "updatedAt",
                "#statusMessage", "statusMessage"
        );

        Map<String, AttributeValue> values = Map.of(
                ":status", AttributeValue.fromS(status.name()),
                ":updatedAt", AttributeValue.fromS(now),
                ":statusMessage", (message == null || message.isBlank())
                        ? AttributeValue.fromS("")
                        : AttributeValue.fromS(message)
        );

        UpdateItemResponse resp = ddb.updateItem(UpdateItemRequest.builder()
                .tableName(jobsTable)
                .key(key)
                .updateExpression("SET #status = :status, #updatedAt = :updatedAt, #statusMessage = :statusMessage")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues(ReturnValue.ALL_NEW)
                .build());

        Map<String, AttributeValue> item = resp.attributes();

        String createdAt = item.getOrDefault("createdAt", AttributeValue.fromS("")).s();
        String inputS3Key = item.containsKey("inputS3Key") ? item.get("inputS3Key").s() : null;
        String updatedAt = item.getOrDefault("updatedAt", AttributeValue.fromS("")).s();
        String statusMessage = item.getOrDefault("statusMessage", AttributeValue.fromS("")).s();

        return new Job(jobId, status.name(), createdAt, inputS3Key, updatedAt, statusMessage);
    }

}