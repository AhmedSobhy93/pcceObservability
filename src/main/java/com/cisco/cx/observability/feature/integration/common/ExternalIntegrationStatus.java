package com.cisco.cx.observability.feature.integration.common;

public record ExternalIntegrationStatus(
        String name,
        String category,
        String state,
        String target,
        String detail) {
}
