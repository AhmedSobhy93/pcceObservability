package com.cisco.cx.observability.model;

import java.util.List;

public record AgentProvisioningRequest(
        String baseUsername,
        String firstName,
        String lastName,
        String displayName,
        String mail,
        String agentId,
        String dn,
        String teamName,
        List<String> skillGroupNames,
        List<String> supervisedTeamNames,
        String deskSettingsName,
        Integer proficiency,
        String userMode,
        String agentType,
        boolean autoRollbackOnError,
        boolean localUser,
        String notes) {
}
