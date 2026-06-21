package com.example.pcceobservability.model;

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
