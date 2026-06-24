package com.example.pcceobservability.model;

public record ProjectTaskUpdateRequest(
        String topic,
        String task,
        String priority,
        Integer priorityNum,
        String status,
        String resource,
        String owner,
        String team,
        String milestone,
        String start,
        String finish,
        Integer duration,
        Integer pct,
        String dependsOn,
        String blockedBy,
        String risk,
        String deliverable,
        String shareWith,
        String externalRef,
        String comments
) {
}
