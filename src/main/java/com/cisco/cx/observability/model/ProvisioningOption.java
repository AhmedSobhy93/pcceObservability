package com.cisco.cx.observability.model;

public record ProvisioningOption(
        String id,
        String name,
        String refUrl,
        String type,
        String changeStamp,
        String detail) {
}
