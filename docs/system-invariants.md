# System Invariants & Non-Goals

This document defines the core guarantees and boundaries of the Cloud Data Platform MVP.
It exists to prevent scope creep and accidental over-engineering.


---

## System Invariants (Must Always Hold)

These are architectural truths that should remain valid as the system evolves.

1. The API service is an orchestrator, not a data processor.
   - All heavy computation must occur in Spark jobs, not in the Spring Boot service.

2. The system is cloud-native and stateless at the API layer.
   - The API service must not store job state locally.

3. All datasets are immutable once submitted.
   - Jobs reference S3 object paths, never modify raw input data.

4. Job execution is asynchronous.
   - API calls must never block on Spark job completion.

5. Infrastructure must be reproducible from code.
   - Any manually created cloud resource is considered technical debt.

6. Failure is a normal state.
   - Job failures must be visible and persisted, not hidden.

---

## Job Lifecycle & Status Transitions

Jobs follow a strict state machine to ensure predictable execution and failure handling.

### Valid Status Transitions

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED
    SUBMITTED --> RUNNING
    RUNNING --> SUCCEEDED
    RUNNING --> FAILED
    SUCCEEDED --> [*]
    FAILED --> [*]
```

### Transition Rules

- **SUBMITTED → RUNNING**: Only valid next state. Job is ready for processing.
- **RUNNING → SUCCEEDED**: Job completed without errors.
- **RUNNING → FAILED**: Job encountered an error during execution.
- **SUCCEEDED / FAILED**: Terminal states. No further transitions allowed.

### Invalid Transitions (Rejected with 400)

- SUBMITTED → SUCCEEDED (must go through RUNNING)
- SUBMITTED → FAILED (must go through RUNNING)
- SUCCEEDED → any state (terminal)
- FAILED → any state (terminal)

### Implementation

Status transitions are validated in `JobStatusTransitionValidator` before being persisted to DynamoDB.
The `updatedAt` timestamp and optional `statusMessage` are updated with each transition.

### Future: Worker Integration

When Spark workers begin reporting job status, they will POST to `/internal/jobs/{jobId}/status`
with the appropriate status update and a descriptive message. The API will validate the transition
and persist the change atomically.

### Nice to haves

- API & Contracts
   - AuthN/Z (OIDC/JWT), rate limiting, request validation, idempotency keys, pagination tokens with opaque cursors.
   - Error contracts and correlation IDs in responses/logs.
- Job Orchestration
   - Worker callbacks with signed requests; retry/backoff; DLQ for failed callbacks.
   - Job timeouts and cancellation semantics.
   - Status audit trail (who/when changed state) beyond current status fields.
- Data Pipeline
   - Output manifest/index for processed artifacts; checksums for integrity.
   - Schema/versioning for inputs and outputs; basic validation before enqueue.
   - Quotas per user/tenant (future multi-tenancy).
- Reliability & Ops
   - Structured logging with trace/span IDs; log redaction.
   - Metrics: request latencies, DynamoDB/S3/EMR calls, job lifecycle durations.
   - Alarms: 5xx rates, WAF findings, DynamoDB throttles, EMR step failures.
   - Runbooks for common failures (upload failures, job stuck RUNNING, EMR step retries).
- Security
   - IAM least-privilege review; split roles for control plane vs. data plane.
   - WAF to BLOCK mode in prod; custom rules (path allowlist, geo/rate tuning).
   - S3 object encryption KMS, bucket key usage; deny public access (already set).
   - Signed worker callbacks; audit logging for status changes.
- Cost & Scaling
   - DynamoDB autoscaling / RCUs+WCUs alarms; GSI capacity alarms.
   - S3 storage class transitions for processed artifacts; tighter lifecycle policies.
   - EMR right-sizing/spot; per-step vs. cluster reuse comparison.
   - ECS scaling policies (CPU/latency-based); task count floor/ceiling.
- DX & Tooling
   - Local dev data fixtures; smoke tests hitting /jobs and /internal/jobs.
   - Postman/Insomnia collection; OpenAPI examples.
   - CI: integration tests with LocalStack for S3/DDB; contract tests for status transitions.
- Data Governance
   - PII scanning on uploads; tagging datasets/jobs; retention policies per dataset type.
   - Access logging (S3 server access logs / CloudTrail data events) for audits.
- Performance
   - Bulk listing with consistent pagination; secondary indexes for filtering by status.
   - Caching presigned URL generation limits; request shaping on upload endpoints.
---

## Explicit Non-Goals (For MVP)

The following are intentionally excluded to preserve focus and delivery speed:

- Real-time streaming ingestion
- User-facing UI
- Complex machine learning pipelines
- Cross-region replication and DR
- Multi-tenant isolation
- Fine-grained RBAC
- Autoscaling optimizations

These may be added in later phases.

---

## Design Philosophy

Prefer:

- Simple over clever
- Explicit over implicit
- Boring technology over trendy abstractions
- Working systems over perfect designs

### Configuration & Environment Variables

The backend service requires the following environment variables in production:

**AWS & Storage:**
- `AWS_REGION`: AWS region (default: `us-east-1`)
- `RAW_BUCKET`: S3 bucket for raw input data (default: `cloud-data-platform-dev-raw`)
- `PROCESSED_BUCKET`: S3 bucket for processed output (default: `cloud-data-platform-dev-processed`)
- `JOBS_TABLE`: DynamoDB table for job metadata (default: `cloud-data-platform-dev-jobs`)

**EMR Serverless:**
- `EMR_APPLICATION_ID`: EMR Serverless application ID (required in production, empty default for local/CI)
- `EMR_JOB_ROLE_ARN`: IAM role ARN for EMR job execution (required in production, empty default for local/CI)

**Local Development & CI:**
All variables have safe defaults in `application.yml` to allow `./gradlew clean build` and test execution without AWS credentials.
In ECS/production, these are injected via task environment variables.

**Spark Job Execution:**
- `SPARK_JAR_PATH`: S3 path to compiled Spark job JAR (default: `s3://cloud-data-platform-dev-raw/jars/cloud-data-platform-spark-jobs-0.1.0.jar`)

All EMR and Spark configuration defaults enable local builds and CI test execution without AWS credentials.
Production deployments override these via ECS task environment variables.

---

## Observability Stack (M7)

The system implements three pillars of observability to enable production operations: request tracing, structured logging, and metrics with alarms.

### 1. Request Tracing via Correlation IDs

Every incoming request is assigned a unique correlation ID to track execution across all services and logs.

**Implementation:**
- `CorrelationIdFilter`: Servlet filter that extracts `X-Correlation-ID` header or generates a UUID.
- Correlation ID is stored in SLF4J's MDC (Mapped Diagnostic Context) for the request lifecycle.
- All log lines automatically include `[%X{correlationId}]` pattern.
- Filter cleans up MDC after request to prevent cross-request contamination.

**Usage:**
```bash
# In your API client, pass the header or let the service generate one:
curl -X GET http://api.cloudpipes.net/jobs \
  -H "X-Correlation-ID: 12345-abcde-67890"

# All logs for this request will include [12345-abcde-67890] in output
```


Debugging: When investigating a user issue, ask for their request's correlation ID and grep CloudWatch logs:

```bash
aws logs filter-log-events \
  --log-group-name /ecs/cloud-data-platform-dev-backend-service \
  --filter-pattern "[%X{correlationId} = 12345-abcde-67890]"
```

### Structured Logging

Logs are emitted in JSON format to CloudWatch for queryability and aggregation.

Configuration:

- application.yml: SLF4J configured with JSON encoder via logback-spring.xml
- Log levels: DEBUG in dev, INFO in production
- Retention: 7 days in CloudWatch (configurable per environment)
- Log groups:
   - /ecs/cloud-data-platform-dev-backend-service: Java application logs
   - /aws/emr-serverless/jobs: EMR Spark job logs (includes driver + executor output)

Key Fields in Logs:

- timestamp: ISO-8601 timestamp
- level: TRACE, DEBUG, INFO, WARN, ERROR
- logger: Full class name (e.g., com.michael.backendservice.jobs.JobsController)
- message: Event description
- correlationId: Request trace ID
- jobId: Job identifier (when applicable)
- stacktrace: Exception details (for ERROR and WARN levels)

Structured Example:

```json
{
  "timestamp": "2026-01-29T14:23:45.123Z",
  "level": "INFO",
  "logger": "com.michael.backendservice.jobs.JobStatusPoller",
  "message": "Job transitioned to SUCCEEDED",
  "correlationId": "req-abc123",
  "jobId": "job-xyz789",
  "processingTimeMs": 1230
}
```

### Custom Metrics & Alarms

The system exports custom metrics to CloudWatch for visibility into API behavior, data pipeline health, and infrastructure state.

#### Metrics (via Micrometer + CloudWatch Registry):


| Metric | Type | Dimension | Use Case |
|--------|------|-----------|----------|
| `jobs.created` | Counter | (none) | Total jobs submitted via API |
| `jobs.completed` | Counter | (none) | Jobs that reached SUCCEEDED or FAILED state |
| `jobs.failed` | Counter | (none) | Jobs that reached FAILED state |
| `emr.job.started` | Counter | (none) | EMR Spark jobs submitted |
| `emr.job.failed` | Counter | (none) | EMR Spark jobs that failed |
| `job.processing.time` | Timer | (none) | Duration from RUNNING → SUCCEEDED/FAILED |

**CloudWatch Alarms:**

| Alarm | Metric | Threshold | Action |
|-------|--------|-----------|--------|
| `cloud-data-platform-dev-api-5xx-errors` | `HTTPCode_Target_5XX_Count` | ≥ 5 in 5 minutes | SNS notification (if configured) |
| `cloud-data-platform-dev-dynamodb-throttle` | `UserErrors` (DynamoDB) | ≥ 1 in 60 seconds | SNS notification |

#### Viewing Metrics:

```bash
# List all alarms
aws cloudwatch describe-alarms \
  --alarm-names cloud-data-platform-dev-api-5xx-errors

# Get metric statistics
aws cloudwatch get-metric-statistics \
  --namespace CloudWatch \
  --metric-name jobs.created \
  --start-time 2026-01-28T00:00:00Z \
  --end-time 2026-01-29T23:59:59Z \
  --period 3600 \
  --statistics Sum
```

#### Interpreting Alarms:

1. 5xx Alarm fires: Check application logs for errors, verify EMR job status, review recent code deployments.

2. DynamoDB Throttle Alarm fires: Check provisioned capacity, review write spike patterns, consider enabling autoscaling (M9.x).
Observability Workflow

**For a stuck job (RUNNING for > 2 hours):**

1. Get the job's correlation ID from DynamoDB or API response
2. Query EMR: aws emr-serverless get-job-run --application-id <id> --job-run-id <emr-id>
3. Check EMR logs: aws logs tail /aws/emr-serverless/jobs --follow
4. Grep application logs: aws logs filter-log-events --filter-pattern "[correlationId]"
5. 5Review metrics: jobs.completed counter should increment if job finishes

**For elevated error rates:**

1. Check alarm: aws cloudwatch describe-alarms --state-reason for recent transitions
2. Correlate with deployments: Check Git logs for recent changes
3. Review slow queries: Check DynamoDB metrics for throttling or high latency
5. Monitor EMR: Verify EMR application is in STARTED state

**Future Enhancements (Not in M7)**
- Distributed Tracing: Add OpenTelemetry/Jaeger for cross-service span propagation (if multi-service architecture emerges)
- Log Aggregation Dashboard: Grafana dashboard pulling CloudWatch metrics and logs
- Alert Escalation: PagerDuty integration for on-call routing
- SLI/SLO Tracking: Define and monitor error budget for job completion rate
- Anomaly Detection: ML-based CloudWatch Anomaly Detector for unexpected metric deviations
