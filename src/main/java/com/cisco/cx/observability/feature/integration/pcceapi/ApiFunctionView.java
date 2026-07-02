package com.cisco.cx.observability.feature.integration.pcceapi;

public record ApiFunctionView(
        String category,
        String function,
        String method,
        String path,
        String description
) {
}
