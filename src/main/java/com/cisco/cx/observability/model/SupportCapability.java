package com.cisco.cx.observability.model;

public record SupportCapability(
        String area,
        String capability,
        String status,
        String action
) {
}
