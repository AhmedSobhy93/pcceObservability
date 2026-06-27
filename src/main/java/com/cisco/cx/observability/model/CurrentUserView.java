package com.cisco.cx.observability.model;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.List;

public record CurrentUserView(
        String username,
        String displayName,
        String agentId,
        List<String> allowedTeams,
        List<AppRole> roles,
        List<Permission> permissions
) {
}
