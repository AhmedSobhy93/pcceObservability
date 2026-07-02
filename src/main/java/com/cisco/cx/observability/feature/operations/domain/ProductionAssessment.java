package com.cisco.cx.observability.feature.operations.domain;

import com.cisco.cx.observability.feature.components.domain.ComponentStatus;
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
