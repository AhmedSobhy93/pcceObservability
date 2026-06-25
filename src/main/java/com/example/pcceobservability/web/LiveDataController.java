package com.example.pcceobservability.web;

import com.example.pcceobservability.model.ExternalIntegrationStatus;
import com.example.pcceobservability.model.RealtimeDataView;
import com.example.pcceobservability.service.LiveDataIntegrationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/live-data")
public class LiveDataController {

    private final LiveDataIntegrationService liveDataIntegrationService;

    public LiveDataController(LiveDataIntegrationService liveDataIntegrationService) {
        this.liveDataIntegrationService = liveDataIntegrationService;
    }

    @GetMapping("/token-probe")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<ExternalIntegrationStatus> tokenProbe() {
        return liveDataIntegrationService.tokenProbe();
    }

    @GetMapping("/realtime-snapshots")
    @PreAuthorize("hasAuthority('PERM_CALL_METRICS_READ')")
    public List<RealtimeDataView> realtimeSnapshots() {
        return liveDataIntegrationService.realtimeSnapshots();
    }
}
