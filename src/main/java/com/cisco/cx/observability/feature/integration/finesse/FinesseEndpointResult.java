package com.cisco.cx.observability.feature.integration.finesse;

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
