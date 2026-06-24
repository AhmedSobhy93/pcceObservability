package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.ComponentName;
import java.time.Instant;

public record ServerMetric(
        ComponentName component,
        String displayName,
        String side,
        String site,
        String tier,
        String host,
        String method,
        String state,
        Double cpuPct,
        Double memoryPct,
        Double diskPct,
        String services,
        String detail,
        Instant checkedAt
) {
}
