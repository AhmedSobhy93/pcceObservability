package com.cisco.cx.observability.feature.integration.common;

public record IntegrationCapability(
        String area,
        String capability,
        String method,
        String status,
        String action
) {
}
