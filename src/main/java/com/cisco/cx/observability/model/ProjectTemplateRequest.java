package com.cisco.cx.observability.model;

public record ProjectTemplateRequest(
        String topic,
        String team,
        String owner,
        String resource,
        String start,
        String finish,
        String shareWith
) {
}
