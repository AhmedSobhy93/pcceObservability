package com.cisco.cx.observability.feature.operations.domain;

public record SupportCapability(
        String area,
        String capability,
        String status,
        String action
) {
}
