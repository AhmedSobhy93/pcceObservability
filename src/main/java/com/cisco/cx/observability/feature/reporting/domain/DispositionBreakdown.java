package com.cisco.cx.observability.feature.reporting.domain;

public record DispositionBreakdown(
        Integer dispositionCode,
        String dispositionName,
        String category,
        boolean countedAsDrop,
        Long calls
) {
}
