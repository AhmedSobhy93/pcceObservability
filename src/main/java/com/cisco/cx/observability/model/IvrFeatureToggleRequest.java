package com.cisco.cx.observability.model;

public record IvrFeatureToggleRequest(
        String appName,
        String featureKey,
        Boolean enabled,
        String minSeverity,
        String configValue,
        String notes) {
}
