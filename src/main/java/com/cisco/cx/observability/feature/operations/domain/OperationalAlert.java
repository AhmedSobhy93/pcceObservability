package com.cisco.cx.observability.feature.operations.domain;

import com.cisco.cx.observability.config.PcceProperties.AlertSeverity;
import java.time.Instant;

public record OperationalAlert(
        String id,
        AlertSeverity severity,
        AlertStatus status,
        String category,
        String component,
        String message,
        String recommendedAction,
        Instant detectedAt
) {
}
