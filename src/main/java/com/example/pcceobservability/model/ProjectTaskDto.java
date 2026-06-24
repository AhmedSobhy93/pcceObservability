package com.example.pcceobservability.model;

public record ProjectTaskDto(
        Long id,
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
    public ProjectTaskDto(
            String topic,
            String task,
            String priority,
            Integer priorityNum,
            String status,
            String resource,
            String start,
            String finish,
            Integer duration,
            Integer pct,
            String comments) {
        this(null, topic, task, priority, priorityNum, status, resource, null, null, null, start, finish,
                duration, pct, null, null, null, null, null, null, comments);
    }
}
