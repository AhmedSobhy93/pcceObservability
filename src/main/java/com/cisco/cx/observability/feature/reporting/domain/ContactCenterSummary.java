package com.cisco.cx.observability.feature.reporting.domain;

import com.cisco.cx.observability.model.ComponentStatus;
import java.math.BigDecimal;
import java.util.List;

public record ContactCenterSummary(
        long callsOffered,
        long callsHandled,
        long callsAbandoned,
        long droppedCalls,
        BigDecimal serviceLevelPct,
        BigDecimal avgHandleTime,
        BigDecimal avgSpeedAnswer,
        List<ComponentStatus> components
) {
}
