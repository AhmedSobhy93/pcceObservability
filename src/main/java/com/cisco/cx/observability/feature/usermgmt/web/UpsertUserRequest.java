package com.cisco.cx.observability.feature.usermgmt.web;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
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
