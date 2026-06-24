package com.example.pcceobservability.model;

public record ExternalIntegrationStatus(
        String name,
        String category,
        String state,
        String target,
        String detail) {
}
