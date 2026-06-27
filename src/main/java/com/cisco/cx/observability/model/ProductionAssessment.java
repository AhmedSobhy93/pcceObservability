package com.cisco.cx.observability.model;

import java.time.Instant;
import java.util.List;


public record ProductionAssessment(
        Instant assessedAt,
        boolean maintenanceMode,
        String maintenanceReason,
        List<OperationalAlert> alerts,
        List<ProductionReadinessItem> readiness,
        List<ComponentStatus> components
) {
}
