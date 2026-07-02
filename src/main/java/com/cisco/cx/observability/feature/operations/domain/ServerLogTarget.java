package com.cisco.cx.observability.feature.operations.domain;

public record ServerLogTarget(
        String component,
        String host,
        String logPath,
        String collectionMethod,
        boolean enabled
) {
}
