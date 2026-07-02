package com.cisco.cx.observability.feature.integration.grafana;

public record GrafanaDashboardView(
        String name,
        String area,
        String url,
        boolean enabled,
        String description
) {
}
