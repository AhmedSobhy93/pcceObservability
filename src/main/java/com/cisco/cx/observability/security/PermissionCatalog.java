package com.cisco.cx.observability.security;

import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.EnumSet;
import java.util.Set;

public final class PermissionCatalog {

    private PermissionCatalog() {
    }

    public static Set<Permission> permissionsFor(AppRole role) {
        return switch (role) {
            case ADMIN -> EnumSet.allOf(Permission.class);
            case WORKFORCE_MANAGER -> EnumSet.of(
                    Permission.CALL_METRICS_READ,
                    Permission.AGENT_STATS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.IVR_METRICS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ);
            case SUPERVISOR -> EnumSet.of(
                    Permission.CALL_METRICS_READ,
                    Permission.AGENT_STATS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ);
            case AGENT -> EnumSet.of(Permission.AGENT_STATS_READ);
            case VIEWER -> EnumSet.of(
                    Permission.CALL_METRICS_READ,
                    Permission.DROPPED_CALLS_READ,
                    Permission.IVR_METRICS_READ,
                    Permission.COMPONENT_STATUS_READ,
                    Permission.OPERATIONS_READ);
        };
    }
}
