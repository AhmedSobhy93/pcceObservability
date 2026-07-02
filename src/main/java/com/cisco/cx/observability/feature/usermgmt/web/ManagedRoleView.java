package com.cisco.cx.observability.feature.usermgmt.web;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.List;

public record ManagedRoleView(
        AppRole role,
        List<Permission> permissions
) {
}
