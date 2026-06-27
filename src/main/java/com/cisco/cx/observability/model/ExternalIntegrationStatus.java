package com.cisco.cx.observability.model;

public record ExternalIntegrationStatus(
        String name,
        String category,
        String state,
        String target,
        String detail) {
}
