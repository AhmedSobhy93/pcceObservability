package com.cisco.cx.observability.model;

public record MaintenanceModeRequest(
        boolean enabled,
        String reason
) {
}
