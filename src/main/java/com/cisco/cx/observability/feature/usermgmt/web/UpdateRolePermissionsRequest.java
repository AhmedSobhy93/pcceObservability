package com.cisco.cx.observability.feature.usermgmt.web;

import com.cisco.cx.observability.config.PcceProperties.Permission;
import java.util.List;

public record UpdateRolePermissionsRequest(
        List<Permission> permissions
) {
}
