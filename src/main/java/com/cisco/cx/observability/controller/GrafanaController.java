package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.model.GrafanaDashboardView;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/grafana")
public class GrafanaController {

    private final PcceProperties pcceProperties;

    public GrafanaController(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    @GetMapping("/dashboards")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<GrafanaDashboardView> dashboards() {
        PcceProperties.Grafana grafana = pcceProperties.getGrafana();
        if (grafana == null || !grafana.isEnabled() || grafana.getDashboards() == null) {
            return List.of();
        }
        return grafana.getDashboards().stream()
                .map(item -> new GrafanaDashboardView(
                        item.getName(),
                        item.getArea(),
                        item.getUrl(),
                        item.isEnabled(),
                        item.getDescription()))
                .toList();
    }
}
