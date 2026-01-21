# Cloud Data Platform (Java + AWS EMR)

A small, production-minded, end-to-end data processing platform built to practice:
- Backend development (Java / Spring Boot)
- Cloud architecture (AWS)
- Data processing (Spark on EMR)
- DevOps practices (Terraform, CI/CD, observability)

## Goal

Users can submit a dataset processing job and retrieve results:
1. Upload dataset (stored in S3)
2. Submit processing job (EMR Spark)
3. Track job status (DynamoDB)
4. Retrieve output artifacts (S3)

## MVP Scope (v1)

### In scope

- Spring Boot API service (Java 21)
- `POST /jobs` submit a job
- `GET /jobs/{jobId}` job status
- Store inputs/outputs in S3
- Store job metadata in DynamoDB
- Run Spark job on EMR
- Terraform for core infrastructure
- CI pipeline to build/test

### Out of scope (for MVP)

- UI
- Authentication/authorization (will add later)
- Streaming ingestion
- Complex ML/AI features
- Multi-tenancy

## High-level architecture

See: `docs/diagrams/architecture.md`

## Repo structure

- `backend-service/` Spring Boot API service
- `spark-jobs/` Spark jobs (Java)
- `infra/` Terraform infrastructure
- `docs/` Architecture notes, ADRs, diagrams

## Running Locally

### Prerequisites

- Java 21 (JDK)
- Git
- macOS or Linux (Windows via WSL also works)

#### Check for Java

```bash
java --version

# Expected Output
openjdk 21 ...
```

#### Start the backend service

From the repository root:

```bash
cd backend-service
./gradlew bootRun
```

#### Verify the service

Health check endpoint:

```bash
curl http://localhost:8080/actuator/health

# Expected response:
{"status":"UP"}
```

Ping endpoint:

```bash
curl http://localhost:8080/ping

# Expected response:
pong
```

#### Notes

- The service is intentionally stateless.

- No AWS resources are required for local execution at this stage.

- EMR, S3, and DynamoDB integration will be added in later milestones.

## Success criteria (MVP)

- Submit a job end-to-end and see it complete successfully
- Outputs written to S3 and retrievable via API
- Basic metrics/logs are visible in CloudWatch
- Infra reproducible via Terraform

## Milestones

- M0: Docs + repo structure
- M1: Spring Boot skeleton (`/ping`, `/health`)
- M2: Terraform creates S3 + DynamoDB
- M3: Thin-slice flow: create job -> store metadata -> upload input -> return jobId
- M4: Add EMR submission + Spark job
- M5: Observability + hardening

## License

MIT