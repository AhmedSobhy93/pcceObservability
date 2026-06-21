package com.example.pcceobservability.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CallMetric(
        LocalDate date,
        Integer hour,
        String skillGroup,
        Long callsOffered,
        Long callsHandled,
        Long callsAbandoned,
        BigDecimal serviceLevelPct,
        BigDecimal avgHandleTime,
        BigDecimal avgTalkTime,
        BigDecimal avgHoldTime,
        BigDecimal avgWrapTime,
        BigDecimal avgSpeedAnswer,
        BigDecimal avgQueueTime,
        BigDecimal maxQueueTime,
        BigDecimal transferRate,
        BigDecimal firstCallResolution,
        BigDecimal ivrContainmentRate,
        BigDecimal csatScore
) {
}
