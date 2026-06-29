package com.cisco.cx.observability.model;

import java.util.List;

public record ProvisioningCatalog(
        boolean pcceApiEnabled,
        boolean cucmAxlEnabled,
        boolean executionEnabled,
        String identityDomain,
        List<ProvisioningOption> agents,
        List<ProvisioningOption> teams,
        List<ProvisioningOption> skillGroups,
        List<ProvisioningOption> deskSettings,
        List<String> warnings) {
}
