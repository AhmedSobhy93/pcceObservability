package com.example.pcceobservability.model;

import com.example.pcceobservability.config.PcceProperties.Permission;
import java.util.List;

public record UpdateRolePermissionsRequest(
        List<Permission> permissions
) {
}
