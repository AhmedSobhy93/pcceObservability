package com.cisco.cx.observability.feature.workforce.web;

import com.cisco.cx.observability.feature.workforce.domain.AgentSkillAssignment;
import com.cisco.cx.observability.feature.workforce.domain.IvrFeatureToggle;
import com.cisco.cx.observability.feature.workforce.service.WorkforceManagementService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workforce-management")
public class WorkforceManagementController {
    private final WorkforceManagementService workforceManagementService;

    public WorkforceManagementController(WorkforceManagementService workforceManagementService) {
        this.workforceManagementService = workforceManagementService;
    }

    @GetMapping("/agent-skill-assignments")
    @PreAuthorize("hasAuthority('PERM_AGENT_STATS_READ') or hasAuthority('PERM_SOLUTION_ADMIN')")
    public List<AgentSkillAssignment> assignments() {
        return workforceManagementService.assignments();
    }

    @PostMapping("/agent-skill-assignments")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public AgentSkillAssignment saveAssignment(@RequestBody AgentSkillAssignmentRequest request) {
        return workforceManagementService.saveAssignment(request);
    }

    @DeleteMapping("/agent-skill-assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public void deleteAssignment(@PathVariable long id) {
        workforceManagementService.deleteAssignment(id);
    }

    @GetMapping("/ivr-features")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ') or hasAuthority('PERM_SOLUTION_ADMIN')")
    public List<IvrFeatureToggle> ivrFeatures() {
        return workforceManagementService.ivrFeatures();
    }

    @PostMapping("/ivr-features")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public IvrFeatureToggle saveIvrFeature(@RequestBody IvrFeatureToggleRequest request) {
        return workforceManagementService.saveIvrFeature(request);
    }

    @DeleteMapping("/ivr-features/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public void deleteIvrFeature(@PathVariable long id) {
        workforceManagementService.deleteIvrFeature(id);
    }
}
