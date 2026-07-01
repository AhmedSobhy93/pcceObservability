package com.cisco.cx.observability.feature.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgentStat(
        LocalDate date,
        String agentName,
        String agentId,
        String team,
        String skillGroup,
        AgentStatus status,
        Long callsHandled,
        BigDecimal avgHandleTime,
        BigDecimal avgTalkTime,
        BigDecimal avgHoldTime,
        BigDecimal avgWrapTime,
        BigDecimal occupancyPct,
        BigDecimal adherencePct,
        Long transfers,
        BigDecimal loginDurationMin,
        BigDecimal notReadyTimeMin
) {
}
