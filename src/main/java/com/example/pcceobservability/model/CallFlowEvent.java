package com.example.pcceobservability.model;

import java.time.LocalDateTime;

public record CallFlowEvent(
        LocalDateTime eventTime,
        String callKey,
        String node,
        String stage,
        String callType,
        String skillGroup,
        String agent,
        String disposition,
        Long durationSeconds,
        String detail
) {
}
