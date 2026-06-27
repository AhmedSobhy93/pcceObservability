package com.cisco.cx.observability.model;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.List;

public record ManagedRoleView(
        AppRole role,
        List<Permission> permissions
) {
}
