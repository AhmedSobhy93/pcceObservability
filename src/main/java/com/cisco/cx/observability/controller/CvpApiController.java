package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.ApiActionRequest;
import com.cisco.cx.observability.model.ApiActionResult;
import com.cisco.cx.observability.model.ApiActionView;
import com.cisco.cx.observability.model.ApiCapability;
import com.cisco.cx.observability.model.ApiFunctionView;
import com.cisco.cx.observability.model.ApiMonitorStatus;
import com.cisco.cx.observability.service.CvpApiMonitoringService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cvp-api")
public class CvpApiController {

    private final CvpApiMonitoringService cvpApiMonitoringService;

    public CvpApiController(CvpApiMonitoringService cvpApiMonitoringService) {
        this.cvpApiMonitoringService = cvpApiMonitoringService;
    }

    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiCapability> capabilities() {
        return cvpApiMonitoringService.capabilities();
    }

    @GetMapping("/functions")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiFunctionView> functions() {
        return cvpApiMonitoringService.functions();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiActionView> actions() {
        return cvpApiMonitoringService.actions();
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiMonitorStatus> status() {
        return cvpApiMonitoringService.status();
    }

    @PostMapping("/actions/{id}/execute")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public ApiActionResult execute(
            @PathVariable String id,
            @RequestBody(required = false) ApiActionRequest request,
            Authentication authentication) {
        ApiActionView action = cvpApiMonitoringService.actions().stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown CVP API action: " + id));
        if ((action.adminOnly() || !"GET".equalsIgnoreCase(action.method())) && !hasAuthority(authentication, "PERM_SOLUTION_ADMIN")) {
            throw new AccessDeniedException("Only solution administrators can execute mutating CVP API actions");
        }
        return cvpApiMonitoringService.execute(
                id,
                request == null ? null : request.body(),
                request == null ? Map.of() : request.pathParams(),
                request == null ? Map.of() : request.queryParams());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
