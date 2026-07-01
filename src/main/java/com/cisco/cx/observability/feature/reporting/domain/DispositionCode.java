package com.cisco.cx.observability.feature.reporting.domain;

public record DispositionCode(
        int code,
        String name,
        String category,
        boolean countedAsDrop
) {
}
