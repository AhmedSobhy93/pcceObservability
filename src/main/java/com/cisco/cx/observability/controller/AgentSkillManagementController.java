package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.AgentProvisioningPlan;
import com.cisco.cx.observability.model.AgentProvisioningRequest;
import com.cisco.cx.observability.model.ProvisioningCatalog;
import com.cisco.cx.observability.service.AgentSkillManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-skill-management")
public class AgentSkillManagementController {
    private final AgentSkillManagementService agentSkillManagementService;

    public AgentSkillManagementController(AgentSkillManagementService agentSkillManagementService) {
        this.agentSkillManagementService = agentSkillManagementService;
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasAuthority('PERM_AGENT_STATS_READ') or hasAuthority('PERM_OPERATIONS_READ') or hasAuthority('PERM_SOLUTION_ADMIN')")
    public ProvisioningCatalog catalog() {
        return agentSkillManagementService.catalog();
    }

    @PostMapping("/plan")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public AgentProvisioningPlan plan(@RequestBody AgentProvisioningRequest request) {
        return agentSkillManagementService.plan(request);
    }

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public AgentProvisioningPlan execute(
            @RequestBody AgentProvisioningRequest request,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        return agentSkillManagementService.execute(request, dryRun);
    }
}
