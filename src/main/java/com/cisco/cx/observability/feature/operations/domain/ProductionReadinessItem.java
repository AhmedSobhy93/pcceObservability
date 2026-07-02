package com.cisco.cx.observability.feature.operations.domain;

public record ProductionReadinessItem(
        String area,
        boolean ready,
        String finding,
        String recommendation
) {
}
