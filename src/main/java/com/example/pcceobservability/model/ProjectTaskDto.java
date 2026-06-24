package com.example.pcceobservability.model;

public record ProjectTaskDto(
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
