package com.example.pcceobservability.web;

import com.example.pcceobservability.model.ApiCapability;
import com.example.pcceobservability.model.ApiFunctionView;
import com.example.pcceobservability.model.ApiMonitorStatus;
import com.example.pcceobservability.service.PcceApiMonitoringService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<ApiMonitorStatus> status() {
        return pcceApiMonitoringService.status();
    }
}
