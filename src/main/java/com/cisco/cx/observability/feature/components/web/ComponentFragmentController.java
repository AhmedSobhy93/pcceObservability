package com.cisco.cx.observability.feature.components.web;

import com.cisco.cx.observability.feature.components.service.ComponentStatusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ComponentFragmentController {

    private final ComponentStatusService componentStatusService;

    public ComponentFragmentController(ComponentStatusService componentStatusService) {
        this.componentStatusService = componentStatusService;
    }

    @GetMapping("/fragments/components/status-list")
    @PreAuthorize("hasAuthority('PERM_COMPONENT_STATUS_READ')")
    public String statusList(Model model) {
        model.addAttribute("components", componentStatusService.status());
        return "feature/components/fragments/status-list :: statusList";
    }
}
