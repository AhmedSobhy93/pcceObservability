package com.cisco.cx.observability.feature.reporting.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DroppedCallMetric(
        LocalDate date,
        Integer hour,
        String skillGroup,
        Long droppedCalls,
        LocalDateTime lastDropTime
) {
}
