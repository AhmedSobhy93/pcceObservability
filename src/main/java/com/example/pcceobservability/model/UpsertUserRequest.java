package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.AppRole;
import com.example.pcceobservability.config.PcceProperties.Permission;
import java.util.List;

public record UpsertUserRequest(
        String password,
        String displayName,
        String agentId,
        Boolean enabled,
        List<String> allowedTeams,
        List<AppRole> roles,
        List<Permission> extraPermissions
) {
}
