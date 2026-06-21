package com.example.pcceobservability.model;

public record MaintenanceModeRequest(
        boolean enabled,
        String reason
) {
}
