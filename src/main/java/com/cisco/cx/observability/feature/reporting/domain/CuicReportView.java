package com.cisco.cx.observability.feature.reporting.domain;

public record CuicReportView(
        String id,
        String name,
        String mode,
        String source,
        String description,
        String endpoint
) {
}
