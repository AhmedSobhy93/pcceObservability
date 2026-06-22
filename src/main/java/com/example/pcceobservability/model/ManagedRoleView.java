package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.AppRole;
import com.example.pcceobservability.config.PcceProperties.Permission;
import java.util.List;

public record ManagedRoleView(
        AppRole role,
        List<Permission> permissions
) {
}
