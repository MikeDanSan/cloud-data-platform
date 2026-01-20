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