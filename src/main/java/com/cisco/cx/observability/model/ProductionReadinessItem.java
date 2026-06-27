package com.cisco.cx.observability.model;

public record ProductionReadinessItem(
        String area,
        boolean ready,
        String finding,
        String recommendation
) {
}
