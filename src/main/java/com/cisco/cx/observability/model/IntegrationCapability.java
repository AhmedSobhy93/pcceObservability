package com.cisco.cx.observability.model;

public record IntegrationCapability(
        String area,
        String capability,
        String method,
        String status,
        String action
) {
}
