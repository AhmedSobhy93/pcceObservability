package com.example.pcceobservability.model;

import java.time.Instant;

public record ApiMonitorStatus(
        String name,
        String category,
        String method,
        String target,
        ComponentState state,
        int statusCode,
        long latencyMs,
        String detail,
        Instant checkedAt
) {
}
