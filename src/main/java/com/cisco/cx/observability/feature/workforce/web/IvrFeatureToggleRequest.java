package com.cisco.cx.observability.feature.workforce.web;

public record IvrFeatureToggleRequest(
        String appName,
        String featureKey,
        Boolean enabled,
        String minSeverity,
        String configValue,
        String notes) {
}
