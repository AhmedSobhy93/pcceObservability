package com.cisco.cx.observability.feature.operations.web;

public record MaintenanceModeRequest(
        boolean enabled,
        String reason
) {
}
