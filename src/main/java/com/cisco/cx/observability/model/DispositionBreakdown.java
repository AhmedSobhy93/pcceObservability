package com.cisco.cx.observability.model;

public record DispositionBreakdown(
        Integer dispositionCode,
        String dispositionName,
        String category,
        boolean countedAsDrop,
        Long calls
) {
}
