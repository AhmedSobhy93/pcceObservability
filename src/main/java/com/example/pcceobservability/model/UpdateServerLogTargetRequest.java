package com.example.pcceobservability.model;

public record UpdateServerLogTargetRequest(
        String component,
        String host,
        String logPath,
        String collectionMethod,
        Boolean enabled
) {
}
