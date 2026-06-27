package com.cisco.cx.observability.model;

public record ServerLogTarget(
        String component,
        String host,
        String logPath,
        String collectionMethod,
        boolean enabled
) {
}
