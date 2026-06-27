package com.cisco.cx.observability.model;

import java.time.Instant;

public record QueryPerformanceEvent(
        Instant timestamp,
        String name,
        long elapsedMs,
        boolean success,
        String error
) {
}
