package com.cisco.cx.observability.feature.workforce.domain;

public record ProvisioningOption(
        String id,
        String name,
        String refUrl,
        String type,
        String changeStamp,
        String detail) {
}
