package com.cisco.cx.observability.feature.monitoring.domain;

import java.time.Instant;

public record QueryPerformanceEvent(
        Instant timestamp,
        String name,
        long elapsedMs,
        boolean success,
        String error
) {
}
