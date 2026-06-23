package com.example.pcceobservability.web;

import com.example.pcceobservability.model.DispositionCode;
import com.example.pcceobservability.model.ReferenceOption;
import com.example.pcceobservability.service.DispositionCodeService;
import com.example.pcceobservability.service.ReportingService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    private final DispositionCodeService dispositionCodeService;
    private final ReportingService reportingService;

    public ReferenceController(DispositionCodeService dispositionCodeService, ReportingService reportingService) {
        this.dispositionCodeService = dispositionCodeService;
        this.reportingService = reportingService;
    }

    @GetMapping("/disposition-codes")
    @PreAuthorize("hasAnyAuthority('PERM_CALL_METRICS_READ', 'PERM_DROPPED_CALLS_READ')")
    public List<DispositionCode> dispositionCodes() {
        return dispositionCodeService.codes();
    }

    @GetMapping("/skill-groups")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<ReferenceOption> skillGroups() {
        return reportingService.skillGroups();
    }

    @GetMapping("/call-types")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<ReferenceOption> callTypes() {
        return reportingService.callTypes();
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAuthority('PERM_AGENT_STATS_READ')")
    public List<ReferenceOption> agents() {
        return reportingService.agents();
    }
}
