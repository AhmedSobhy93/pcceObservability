package com.cisco.cx.observability.feature.workforce.domain;

import java.time.LocalDateTime;

public record IvrFeatureToggle(
        Long id,
        String appName,
        String featureKey,
        boolean enabled,
        String minSeverity,
        String configValue,
        String notes,
        LocalDateTime updatedAt) {
}
