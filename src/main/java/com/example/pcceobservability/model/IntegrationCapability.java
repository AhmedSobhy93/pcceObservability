package com.example.pcceobservability.model;

public record IntegrationCapability(
        String area,
        String capability,
        String method,
        String status,
        String action
) {
}
