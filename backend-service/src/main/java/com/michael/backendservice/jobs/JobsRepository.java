package com.michael.backendservice.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        if (inputS3Key != null && !inputS3Key.isBlank()) {
            item.put("inputS3Key", AttributeValue.fromS(inputS3Key));
        }

        ddb.putItem(PutItemRequest.builder()
                .tableName(jobsTable)
                .item(item)
                .build());

        return new Job(jobId, JobStatus.SUBMITTED.name(), now.toString(), inputS3Key, now.toString(), null, null);
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

        return Optional.of(mapToJob(resp.item()));
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

    public Job updateStatus(String jobId, JobStatus status, String message, String outputS3Key) {
        String now = Instant.now().toString();
        Map<String, AttributeValue> key = Map.of("jobId", AttributeValue.fromS(jobId));

        StringBuilder setExpr = new StringBuilder("SET #status = :status, #updatedAt = :updatedAt");
        Map<String, String> names = new HashMap<>();
        names.put("#status", "status");
        names.put("#updatedAt", "updatedAt");

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":status", AttributeValue.fromS(status.name()));
        values.put(":updatedAt", AttributeValue.fromS(now));

        if (message != null && !message.isBlank()) {
            setExpr.append(", #statusMessage = :statusMessage");
            names.put("#statusMessage", "statusMessage");
            values.put(":statusMessage", AttributeValue.fromS(message));
        } else {
            names.put("#statusMessage", "statusMessage");
        }

        if (outputS3Key != null && !outputS3Key.isBlank()) {
            setExpr.append(", #outputS3Key = :outputS3Key");
            names.put("#outputS3Key", "outputS3Key");
            values.put(":outputS3Key", AttributeValue.fromS(outputS3Key));
        } else {
            names.put("#outputS3Key", "outputS3Key");
        }

        UpdateItemRequest.Builder req = UpdateItemRequest.builder()
                .tableName(jobsTable)
                .key(key)
                .updateExpression(setExpr.toString())
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .returnValues("ALL_NEW");

        UpdateItemResponse resp = ddb.updateItem(req.build());

        return mapToJob(resp.attributes());
    }

    public JobsPage listJobs(Integer limit, String lastEvaluatedJobId) {
        ScanRequest.Builder builder = ScanRequest.builder()
                .tableName(jobsTable);

        if (limit != null && limit > 0) {
            builder.limit(limit);
        }

        if (lastEvaluatedJobId != null && !lastEvaluatedJobId.isBlank()) {
            builder.exclusiveStartKey(Map.of("jobId", AttributeValue.fromS(lastEvaluatedJobId)));
        }

        ScanResponse resp = ddb.scan(builder.build());

        List<Job> items = new ArrayList<>();
        for (Map<String, AttributeValue> item : resp.items()) {
            items.add(mapToJob(item));
        }

        String nextKey = null;
        if (resp.hasLastEvaluatedKey() && resp.lastEvaluatedKey().containsKey("jobId")) {
            nextKey = resp.lastEvaluatedKey().get("jobId").s();
        }

        return new JobsPage(items, nextKey);
    }

    private Job mapToJob(Map<String, AttributeValue> item) {
        String jobId = item.getOrDefault("jobId", AttributeValue.fromS("")).s();
        String status = item.getOrDefault("status", AttributeValue.fromS("UNKNOWN")).s();
        String createdAt = item.getOrDefault("createdAt", AttributeValue.fromS("")).s();
        String inputS3Key = item.containsKey("inputS3Key") ? item.get("inputS3Key").s() : null;
        String updatedAt = item.containsKey("updatedAt") ? item.get("updatedAt").s() : null;
        String statusMessage = item.containsKey("statusMessage") ? item.get("statusMessage").s() : null;
        String outputS3Key = item.containsKey("outputS3Key") ? item.get("outputS3Key").s() : null;

        return new Job(jobId, status, createdAt, inputS3Key, updatedAt, statusMessage, outputS3Key);
    }
}