package com.cisco.cx.observability.model;

public record UpdateServerLogTargetRequest(
        String component,
        String host,
        String logPath,
        String collectionMethod,
        Boolean enabled
) {
}
