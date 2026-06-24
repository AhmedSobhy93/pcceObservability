package com.example.pcceobservability.model;

public record ProjectTaskUpdateRequest(
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
        String comments
) {
}
