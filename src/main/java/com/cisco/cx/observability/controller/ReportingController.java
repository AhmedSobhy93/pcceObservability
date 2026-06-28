package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.AgentStat;
import com.cisco.cx.observability.model.CallMetric;
import com.cisco.cx.observability.model.CallFlowEvent;
import com.cisco.cx.observability.model.CallTypeMetric;
import com.cisco.cx.observability.model.ContactCenterSummary;
import com.cisco.cx.observability.model.CvpIvrNodeMetric;
import com.cisco.cx.observability.model.CuicReportView;
import com.cisco.cx.observability.model.DispositionBreakdown;
import com.cisco.cx.observability.model.DroppedCallMetric;
import com.cisco.cx.observability.service.ReportingService;
import com.cisco.cx.observability.service.ReportingService.IvrContainmentMetric;
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

    @GetMapping("/metrics/call-types")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<CallTypeMetric> callTypeMetrics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) String skillGroup) {
        return reportingService.callTypeMetrics(from, to, callType, skillGroup);
    }

    @GetMapping("/cuic/reports")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<CuicReportView> cuicReports() {
        return reportingService.cuicReports();
    }

    @GetMapping("/calls/flow")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<CallFlowEvent> callFlow(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String callKey,
            @RequestParam(required = false) String agentId) {
        return reportingService.callFlow(from, to, callKey, agentId);
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

    @GetMapping("/metrics/cvp-ivr-nodes")
    @PreAuthorize("hasAuthority('PERM_IVR_METRICS_READ')")
    public List<CvpIvrNodeMetric> cvpIvrNodes(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String appName) {
        return reportingService.cvpIvrNodes(from, to, appName);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ') and hasAuthority('PERM_DROPPED_CALLS_READ') and hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public ContactCenterSummary summary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summary(from, to);
    }
}
