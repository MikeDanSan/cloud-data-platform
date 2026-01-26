package com.michael.backendservice.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JobStatusTransitionValidator {

    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        // SUBMITTED can transition to RUNNING
        VALID_TRANSITIONS.put(JobStatus.SUBMITTED, Set.of(JobStatus.RUNNING));

        // RUNNING can transition to SUCCEEDED or FAILED
        VALID_TRANSITIONS.put(JobStatus.RUNNING, Set.of(JobStatus.SUCCEEDED, JobStatus.FAILED));

        // Terminal states: SUCCEEDED and FAILED cannot transition
        VALID_TRANSITIONS.put(JobStatus.SUCCEEDED, Set.of());
        VALID_TRANSITIONS.put(JobStatus.FAILED, Set.of());
    }

    /**
     * Validates if a transition from currentStatus to newStatus is allowed.
     *
     * @param currentStatus the current job status
     * @param newStatus     the desired new status
     * @return true if transition is valid, false otherwise
     */
    public static boolean isValidTransition(JobStatus currentStatus, JobStatus newStatus) {
        return VALID_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(newStatus);
    }

    /**
     * Gets the allowed transitions from the given status.
     *
     * @param currentStatus the current job status
     * @return a set of allowed next statuses
     */
    public static Set<JobStatus> getAllowedTransitions(JobStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }
}