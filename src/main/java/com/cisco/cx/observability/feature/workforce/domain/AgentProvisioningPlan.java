package com.cisco.cx.observability.feature.workforce.domain;

import java.util.List;

public record AgentProvisioningPlan(
        String cucmUserId,
        String pcceUserName,
        String displayName,
        String dn,
        String deviceName,
        String mode,
        String agentType,
        boolean dryRun,
        boolean executionEnabled,
        List<ProvisioningStepResult> steps,
        List<String> warnings) {
}
