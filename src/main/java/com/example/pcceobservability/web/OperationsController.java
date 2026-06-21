package com.example.pcceobservability.web;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.AuditEvent;
import com.example.pcceobservability.model.MaintenanceModeRequest;
import com.example.pcceobservability.model.ProductionAssessment;
import com.example.pcceobservability.service.AuditService;
import com.example.pcceobservability.service.OperationsService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationsController {

    private final OperationsService operationsService;
    private final AuditService auditService;
    private final PcceProperties pcceProperties;

    public OperationsController(
            OperationsService operationsService,
            AuditService auditService,
            PcceProperties pcceProperties) {
        this.operationsService = operationsService;
        this.auditService = auditService;
        this.pcceProperties = pcceProperties;
    }

    @GetMapping("/assessment")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public ProductionAssessment assessment(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return operationsService.assess(from, to);
    }

    @GetMapping("/assessment/last")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public ProductionAssessment lastAssessment() {
        return operationsService.lastAssessment();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public List<AuditEvent> audit(@RequestParam(defaultValue = "100") int limit) {
        return auditService.recent(limit);
    }

    @PutMapping("/maintenance")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public Map<String, Object> maintenance(@RequestBody MaintenanceModeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        pcceProperties.getOperations().setMaintenanceMode(request.enabled());
        pcceProperties.getOperations().setMaintenanceReason(request.reason());
        auditService.record("SET_MAINTENANCE_MODE", "operations", Map.of(
                "enabled", request.enabled(),
                "reason", request.reason() == null ? "" : request.reason()));
        return Map.of(
                "maintenanceMode", pcceProperties.getOperations().isMaintenanceMode(),
                "maintenanceReason", pcceProperties.getOperations().getMaintenanceReason() == null
                        ? ""
                        : pcceProperties.getOperations().getMaintenanceReason());
    }
}
