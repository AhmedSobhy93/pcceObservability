package com.cisco.cx.observability.feature.integration.pcceapi;

import java.util.Map;

public record ApiActionRequest(
        String body,
        Map<String, String> pathParams,
        Map<String, String> queryParams
) {
}
