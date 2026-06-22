package com.example.pcceobservability.web;

import com.example.pcceobservability.model.AgentStat;
import com.example.pcceobservability.model.CallMetric;
import com.example.pcceobservability.model.ContactCenterSummary;
import com.example.pcceobservability.model.DispositionBreakdown;
import com.example.pcceobservability.model.DroppedCallMetric;
import com.example.pcceobservability.service.ReportingService;
import com.example.pcceobservability.service.ReportingService.IvrContainmentMetric;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/metrics/calls")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<CallMetric> callMetrics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String skillGroup) {
        return reportingService.callMetrics(from, to, skillGroup);
    }

    @GetMapping("/agents/stats")
    @PreAuthorize("hasAuthority('PERM_AGENT_STATS_READ')")
    public List<AgentStat> agentStats(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String team) {
        return reportingService.agentStats(from, to, agentId, team);
    }

    @GetMapping("/calls/dropped")
    @PreAuthorize("hasAuthority('PERM_DROPPED_CALLS_READ')")
    public List<DroppedCallMetric> droppedCalls(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String skillGroup) {
        return reportingService.droppedCalls(from, to, skillGroup);
    }

    @GetMapping("/calls/disposition-breakdown")
    @PreAuthorize("hasAuthority('PERM_DROPPED_CALLS_READ')")
    public List<DispositionBreakdown> dispositionBreakdown(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.dispositionBreakdown(from, to);
    }

    @GetMapping("/metrics/ivr-containment")
    @PreAuthorize("hasAuthority('PERM_IVR_METRICS_READ')")
    public List<IvrContainmentMetric> ivrContainment(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.ivrContainment(from, to);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ') and hasAuthority('PERM_DROPPED_CALLS_READ') and hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public ContactCenterSummary summary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summary(from, to);
    }
}
