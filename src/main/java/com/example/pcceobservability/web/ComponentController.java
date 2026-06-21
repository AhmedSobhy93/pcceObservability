package com.example.pcceobservability.web;

import com.example.pcceobservability.model.ComponentStatus;
import com.example.pcceobservability.service.ComponentStatusService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/components")
public class ComponentController {

    private final ComponentStatusService componentStatusService;

    public ComponentController(ComponentStatusService componentStatusService) {
        this.componentStatusService = componentStatusService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public List<ComponentStatus> status() {
        return componentStatusService.status();
    }
}
