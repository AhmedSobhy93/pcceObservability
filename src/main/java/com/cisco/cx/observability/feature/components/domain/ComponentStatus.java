package com.cisco.cx.observability.feature.components.domain;

import com.cisco.cx.observability.config.PcceProperties.ComponentName;
import com.cisco.cx.observability.config.PcceProperties.ProbeType;
import com.cisco.cx.observability.shared.domain.ComponentState;
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
