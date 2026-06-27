package com.cisco.cx.observability.model;

import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.List;

public record UpdateRolePermissionsRequest(
        List<Permission> permissions
) {
}
