package com.cisco.cx.observability.feature.workforce.web;

public record AgentSkillAssignmentRequest(
        String agentId,
        String agentName,
        String teamName,
        String skillGroup,
        Integer proficiency,
        Boolean enabled,
        String source,
        String notes) {
}
