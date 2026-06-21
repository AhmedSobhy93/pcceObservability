package com.example.pcceobservability.model;

public record ProductionReadinessItem(
        String area,
        boolean ready,
        String finding,
        String recommendation
) {
}
