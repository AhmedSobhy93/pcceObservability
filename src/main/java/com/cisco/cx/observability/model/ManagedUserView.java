package com.cisco.cx.observability.model;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
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
