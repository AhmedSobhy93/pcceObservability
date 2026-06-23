package com.example.pcceobservability.model;

import java.time.Instant;

public record FinesseEndpointResult(
        String name,
        String method,
        String target,
        int statusCode,
        long latencyMs,
        String body,
        Instant checkedAt
) {
}
