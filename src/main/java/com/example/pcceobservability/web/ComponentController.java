package com.example.pcceobservability.web;

import com.example.pcceobservability.model.ComponentStatus;
import com.example.pcceobservability.config.PcceProperties.ComponentName;
import com.example.pcceobservability.service.ComponentHistoryService;
import com.example.pcceobservability.service.ComponentStatusService;
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

    public ComponentController(
            ComponentStatusService componentStatusService,
            ComponentHistoryService componentHistoryService) {
        this.componentStatusService = componentStatusService;
        this.componentHistoryService = componentHistoryService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ComponentStatus> status() {
        return componentStatusService.status();
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
