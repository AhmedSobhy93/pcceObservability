package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.ComponentName;
import com.example.pcceobservability.config.PcceProperties.ProbeType;
import java.time.Instant;

public record ComponentStatus(
        ComponentName name,
        String displayName,
        String side,
        String site,
        String tier,
        ComponentState state,
        ProbeType probe,
        String target,
        long latencyMs,
        String detail,
        Instant checkedAt
) {
}
