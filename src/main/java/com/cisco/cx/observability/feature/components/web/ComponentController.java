package com.cisco.cx.observability.feature.components.web;

import com.cisco.cx.observability.feature.components.domain.ComponentStatus;
import com.cisco.cx.observability.feature.components.service.ComponentHistoryService;
import com.cisco.cx.observability.feature.components.service.ComponentStatusService;
import com.cisco.cx.observability.feature.monitoring.domain.ServerMetric;
import com.cisco.cx.observability.feature.monitoring.service.ServerMetricService;
import com.cisco.cx.observability.config.PcceProperties.ComponentName;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/components")
public class ComponentController {

    private final ComponentStatusService componentStatusService;
    private final ComponentHistoryService componentHistoryService;
    private final ServerMetricService serverMetricService;

    public ComponentController(
            ComponentStatusService componentStatusService,
            ComponentHistoryService componentHistoryService,
            ServerMetricService serverMetricService) {
        this.componentStatusService = componentStatusService;
        this.componentHistoryService = componentHistoryService;
        this.serverMetricService = serverMetricService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ComponentStatus> status() {
        return componentStatusService.status();
    }

    @GetMapping("/server-metrics")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ServerMetric> serverMetrics() {
        return serverMetricService.metrics();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ComponentStatus> history(@RequestParam(name = "limit", required = false) Integer limit) {
        return componentHistoryService.recent(limit == null ? 100 : limit);
    }

    @GetMapping("/{name}/history")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ComponentStatus> history(
            @PathVariable("name") ComponentName name,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return componentHistoryService.recent(name, limit == null ? 100 : limit);
    }
}
