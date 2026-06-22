package com.example.pcceobservability.model;

public record DispositionCode(
        int code,
        String name,
        String category,
        boolean countedAsDrop
) {
}
