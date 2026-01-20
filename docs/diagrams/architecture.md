# Architecture (MVP)

This diagram represents the MVP flow. It intentionally prioritizes clarity over completeness.

```mermaid
flowchart LR
  U[Client / CLI] --> API[Spring Boot API Service]

  API -->|Upload raw dataset| S3IN[(S3 Raw Bucket)]
  API -->|Create/Update job| DDB[(DynamoDB Jobs Table)]
  API -->|Submit Spark step| EMR[EMR Cluster / Step]

  EMR -->|Read input| S3IN
  EMR -->|Write output| S3OUT[(S3 Processed Bucket)]
  EMR -->|Update status| DDB

  API --> CW[(CloudWatch Logs/Metrics)]
  EMR --> CW

```
### Note

For MVP, job status updates can be driven either by:

 - The Spark job writing status back to DynamoDB, or

 - A lightweight polling/step-tracking mechanism in the API service.

Authentication is intentionally excluded from MVP to maintain momentum.