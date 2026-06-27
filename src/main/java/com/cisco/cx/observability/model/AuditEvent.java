package com.cisco.cx.observability.model;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
        Instant timestamp,
        String actor,
        String action,
        String target,
        Map<String, Object> details
) {
}
