package com.cisco.cx.observability.feature.integration.pcceapi;

import com.cisco.cx.observability.feature.operations.domain.RtmtCapability;
import com.cisco.cx.observability.feature.operations.domain.SpogCapability;
import java.util.List;
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
@RequestMapping("/api/v1/pcce-api")
public class PcceApiController {

    private final PcceApiMonitoringService pcceApiMonitoringService;

    public PcceApiController(PcceApiMonitoringService pcceApiMonitoringService) {
        this.pcceApiMonitoringService = pcceApiMonitoringService;
    }

    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiCapability> capabilities() {
        return pcceApiMonitoringService.capabilities();
    }

    @GetMapping("/functions")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiFunctionView> functions() {
        return pcceApiMonitoringService.functions();
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiActionView> actions() {
        return pcceApiMonitoringService.actions();
    }

    @PostMapping("/actions/{id}/execute")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public ApiActionResult execute(
            @PathVariable String id,
            @RequestBody(required = false) ApiActionRequest request,
            Authentication authentication) {
        ApiActionView action = pcceApiMonitoringService.actions().stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PCCE API action: " + id));
        if ((action.adminOnly() || !"GET".equalsIgnoreCase(action.method())) && !hasAuthority(authentication, "PERM_SOLUTION_ADMIN")) {
            throw new AccessDeniedException("Only solution administrators can execute mutating PCCE API actions");
        }
        return pcceApiMonitoringService.execute(
                id,
                request == null ? null : request.body(),
                request == null ? java.util.Map.of() : request.pathParams(),
                request == null ? java.util.Map.of() : request.queryParams());
    }

    @GetMapping("/rtmt-capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<RtmtCapability> rtmtCapabilities() {
        return pcceApiMonitoringService.rtmtCapabilities();
    }

    @GetMapping("/spog-capabilities")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<SpogCapability> spogCapabilities() {
        return pcceApiMonitoringService.spogCapabilities();
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiMonitorStatus> status() {
        return pcceApiMonitoringService.status();
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
