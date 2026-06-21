package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.AlertSeverity;
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
