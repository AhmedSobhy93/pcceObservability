package com.cisco.cx.observability.model;

public record ProvisioningStepResult(
        int order,
        String system,
        String action,
        String method,
        String target,
        String status,
        int statusCode,
        long latencyMs,
        String detail,
        String payloadPreview) {
}
