package com.cisco.cx.observability.model;

public record CuicReportView(
        String id,
        String name,
        String mode,
        String source,
        String description,
        String endpoint
) {
}
