package com.cisco.cx.observability.model;

public record DispositionCode(
        int code,
        String name,
        String category,
        boolean countedAsDrop
) {
}
