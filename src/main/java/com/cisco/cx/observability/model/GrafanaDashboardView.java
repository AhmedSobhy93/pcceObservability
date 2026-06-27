package com.cisco.cx.observability.model;

public record GrafanaDashboardView(
        String name,
        String area,
        String url,
        boolean enabled,
        String description
) {
}
