# ADR 0001: Java/Spring Boot API + EMR/Spark + DynamoDB for job metadata

## Status

Accepted

## Context

This project aims to build an end-to-end data processing platform that improves skills in:
- Software engineering (backend service)
- Architecture (cloud-native data processing)
- DevOps (IaC, CI/CD, observability)

The author currently works with a large Java microservice system and AWS-based data platforms.

## Decision

Use:

- Java 21 + Spring Boot 3 for the API service
- AWS S3 for input/output datasets
- AWS EMR (Spark) for batch processing
- DynamoDB for job metadata (status, pointers to S3 input/output)

## Rationale

- Java aligns with current professional environment and strengthens backend competency.
- EMR/Spark reflects real-world enterprise batch processing patterns.
- DynamoDB is simple, scalable, and cost-effective for job metadata in an MVP.
- S3 provides durable, inexpensive storage for datasets and artifacts.

## Consequences

### Positive

- Strong alignment with enterprise patterns and current role.
- Clear separation of concerns: orchestration in API, compute in Spark.
- Infrastructure is modular and reproducible.

### Negative / Risks

- EMR adds cost and operational complexity.
- Java ecosystem choices can lead to over-engineering if scope isn’t controlled.
- DynamoDB modeling needs care (partition key design).

## Alternatives considered

- AWS Glue instead of EMR (simpler ops, less control)
- RDS/Postgres instead of DynamoDB (strong relational modeling, more ops)
- Python/FastAPI backend (faster iteration, less aligned with current Java stack)