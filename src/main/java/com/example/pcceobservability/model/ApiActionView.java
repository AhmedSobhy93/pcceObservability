package com.example.pcceobservability.model;

public record ApiActionView(
        String id,
        String category,
        String name,
        boolean enabled,
        boolean adminOnly,
        String method,
        String path,
        String contentType
) {
}
