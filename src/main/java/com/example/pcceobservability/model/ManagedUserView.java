package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.AppRole;
import com.example.pcceobservability.config.PcceProperties.Permission;
import java.util.List;

public record ManagedUserView(
        String username,
        String displayName,
        String agentId,
        boolean enabled,
        List<String> allowedTeams,
        List<AppRole> roles,
        List<Permission> extraPermissions
) {
}
