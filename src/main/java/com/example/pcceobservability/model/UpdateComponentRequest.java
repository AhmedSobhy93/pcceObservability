package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.ProbeType;

public record UpdateComponentRequest(
        Boolean enabled,
        String displayName,
        String side,
        String site,
        String tier,
        ProbeType probe,
        String host,
        Integer port,
        String url,
        Long timeoutSeconds,
        Boolean trustAllCertificates,
        Integer expectedStatusMin,
        Integer expectedStatusMax
) {
}
