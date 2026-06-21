package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.ProbeType;

public record UpdateComponentRequest(
        Boolean enabled,
        ProbeType probe,
        String host,
        Integer port,
        String url,
        Long timeoutSeconds
) {
}
