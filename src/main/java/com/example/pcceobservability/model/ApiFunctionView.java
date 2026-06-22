package com.example.pcceobservability.model;

public record ApiFunctionView(
        String category,
        String function,
        String method,
        String path,
        String description
) {
}
