package com.example.pcceobservability.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RealtimeDataView(
        String name,
        String source,
        String state,
        String description,
        List<Map<String, Object>> rows,
        String error,
        Instant checkedAt) {
}
