package com.cisco.cx.observability.model;

import java.time.LocalDate;

public record CallTypeMetric(
        LocalDate date,
        Integer hour,
        String callType,
        String skillGroup,
        Long calls,
        Long handledCalls
) {
}
