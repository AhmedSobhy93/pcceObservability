package com.example.pcceobservability.model;

import java.time.Instant;

public record ApiActionResult(
        String id,
        String method,
        String target,
        int statusCode,
        long latencyMs,
        String body,
        Instant executedAt
) {
}
