package com.cisco.cx.observability.model;

import java.util.Map;

public record ApiActionRequest(
        String body,
        Map<String, String> pathParams,
        Map<String, String> queryParams
) {
}
