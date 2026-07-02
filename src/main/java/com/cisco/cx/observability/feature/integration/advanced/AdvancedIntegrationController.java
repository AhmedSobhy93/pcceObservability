package com.cisco.cx.observability.feature.integration.advanced;

import com.cisco.cx.observability.feature.integration.common.ExternalIntegrationStatus;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/advanced")
public class AdvancedIntegrationController {

    private final AdvancedIntegrationService advancedIntegrationService;

    public AdvancedIntegrationController(AdvancedIntegrationService advancedIntegrationService) {
        this.advancedIntegrationService = advancedIntegrationService;
    }

    @GetMapping("/jmx")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ExternalIntegrationStatus> jmx() {
        return advancedIntegrationService.jmx();
    }

    @GetMapping("/app-dynamics")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ExternalIntegrationStatus> appDynamics() {
        return advancedIntegrationService.appDynamics();
    }

    @GetMapping("/live-data")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<ExternalIntegrationStatus> liveData() {
        return advancedIntegrationService.liveData();
    }
}
