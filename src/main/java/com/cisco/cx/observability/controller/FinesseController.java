package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.FinesseEndpointResult;
import com.cisco.cx.observability.model.IntegrationCapability;
import com.cisco.cx.observability.service.FinesseIntegrationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finesse")
public class FinesseController {

    private final FinesseIntegrationService finesseIntegrationService;

    public FinesseController(FinesseIntegrationService finesseIntegrationService) {
        this.finesseIntegrationService = finesseIntegrationService;
    }

    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ') or hasAuthority('PERM_AGENT_STATS_READ')")
    public List<IntegrationCapability> capabilities() {
        return finesseIntegrationService.capabilities();
    }

    @GetMapping("/system")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<FinesseEndpointResult> system() {
        return finesseIntegrationService.system();
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ') or hasAuthority('PERM_AGENT_STATS_READ')")
    public List<FinesseEndpointResult> agents() {
        return finesseIntegrationService.agents();
    }

    @GetMapping("/dialogs")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ') or hasAuthority('PERM_AGENT_STATS_READ')")
    public List<FinesseEndpointResult> dialogs() {
        return finesseIntegrationService.dialogs();
    }

    @GetMapping("/teams")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<FinesseEndpointResult> teams() {
        return finesseIntegrationService.teams();
    }

    @GetMapping("/queues")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<FinesseEndpointResult> queues() {
        return finesseIntegrationService.queues();
    }
}
