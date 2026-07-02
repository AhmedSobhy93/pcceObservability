package com.cisco.cx.observability.feature.workforce.domain;

import java.time.LocalDateTime;

public record AgentSkillAssignment(
        Long id,
        String agentId,
        String agentName,
        String teamName,
        String skillGroup,
        Integer proficiency,
        boolean enabled,
        String source,
        String notes,
        LocalDateTime updatedAt) {
}
