package com.example.pcceobservability.model;

public record GrafanaDashboardView(
        String name,
        String area,
        String url,
        boolean enabled,
        String description
) {
}
