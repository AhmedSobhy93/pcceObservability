package com.example.pcceobservability.model;

public record CuicReportView(
        String id,
        String name,
        String mode,
        String source,
        String description,
        String endpoint
) {
}
