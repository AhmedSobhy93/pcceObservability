package com.cisco.cx.observability.feature.operations.web;

public record UpdateServerLogTargetRequest(
        String component,
        String host,
        String logPath,
        String collectionMethod,
        Boolean enabled
) {
}
