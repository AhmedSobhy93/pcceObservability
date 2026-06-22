package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.config.PcceProperties.AlertSeverity;
import com.example.pcceobservability.model.AlertStatus;
import com.example.pcceobservability.model.CallMetric;
import com.example.pcceobservability.model.ComponentState;
import com.example.pcceobservability.model.ComponentStatus;
import com.example.pcceobservability.model.DroppedCallMetric;
import com.example.pcceobservability.model.OperationalAlert;
import com.example.pcceobservability.model.ProductionAssessment;
import com.example.pcceobservability.model.ProductionReadinessItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OperationsService {

    private final PcceProperties pcceProperties;
    private final ComponentStatusService componentStatusService;
    private final ReportingService reportingService;
    private final AlertNotificationService alertNotificationService;
    private volatile ProductionAssessment lastAssessment;

    public OperationsService(
            PcceProperties pcceProperties,
            ComponentStatusService componentStatusService,
            ReportingService reportingService,
            AlertNotificationService alertNotificationService) {
        this.pcceProperties = pcceProperties;
        this.componentStatusService = componentStatusService;
        this.reportingService = reportingService;
        this.alertNotificationService = alertNotificationService;
    }

    public ProductionAssessment assess(LocalDate from, LocalDate to) {
        List<ComponentStatus> components = componentStatusService.status();
        List<OperationalAlert> alerts = new ArrayList<>();
        alerts.addAll(componentAlerts(components));
        alerts.addAll(metricAlerts(from, to));

        boolean maintenanceMode = pcceProperties.getOperations().isMaintenanceMode();
        if (maintenanceMode) {
            alerts = alerts.stream()
                    .map(alert -> new OperationalAlert(
                            alert.id(),
                            alert.severity(),
                            AlertStatus.SUPPRESSED,
                            alert.category(),
                            alert.component(),
                            alert.message(),
                            "Maintenance mode is enabled: " + pcceProperties.getOperations().getMaintenanceReason(),
                            alert.detectedAt()))
                    .toList();
        }

        ProductionAssessment assessment = new ProductionAssessment(
                Instant.now(),
                maintenanceMode,
                pcceProperties.getOperations().getMaintenanceReason(),
                alerts,
                readiness(components),
                components);
        lastAssessment = assessment;
        alertNotificationService.notifyAssessment(assessment);
        return assessment;
    }

    public ProductionAssessment lastAssessment() {
        return lastAssessment;
    }

    @Scheduled(fixedDelayString = "${pcce.operations.assessment-fixed-delay-ms:300000}")
    public void scheduledAssessment() {
        if (pcceProperties.getOperations().isScheduledAssessmentEnabled()) {
            assess(LocalDate.now(), LocalDate.now());
        }
    }

    private List<OperationalAlert> componentAlerts(List<ComponentStatus> components) {
        List<OperationalAlert> alerts = new ArrayList<>();
        for (ComponentStatus component : components) {
            if (component.state() == ComponentState.DOWN) {
                alerts.add(alert(
                        AlertSeverity.CRITICAL,
                        "COMPONENT",
                        component.name().name(),
                        component.name() + " is DOWN: " + component.detail(),
                        "Check service state, network path, firewall rules, and Cisco component logs."));
            } else if (component.state() == ComponentState.UP
                    && component.latencyMs() >= pcceProperties.getOperations().getComponentLatencyCriticalMs()) {
                alerts.add(alert(
                        AlertSeverity.CRITICAL,
                        "LATENCY",
                        component.name().name(),
                        component.name() + " latency is " + component.latencyMs() + "ms",
                        "Investigate network latency, SQL blocking, CPU pressure, and disk latency."));
            } else if (component.state() == ComponentState.UP
                    && component.latencyMs() >= pcceProperties.getOperations().getComponentLatencyWarningMs()) {
                alerts.add(alert(
                        AlertSeverity.WARNING,
                        "LATENCY",
                        component.name().name(),
                        component.name() + " latency is " + component.latencyMs() + "ms",
                        "Review trend and validate no active incident is developing."));
            }
        }
        return alerts;
    }

    private List<OperationalAlert> metricAlerts(LocalDate from, LocalDate to) {
        List<OperationalAlert> alerts = new ArrayList<>();
        try {
            List<DroppedCallMetric> droppedCalls = reportingService.droppedCalls(from, to, null);
            long totalDropped = droppedCalls.stream().mapToLong(metric -> metric.droppedCalls() == null ? 0 : metric.droppedCalls()).sum();
            if (totalDropped >= pcceProperties.getOperations().getDroppedCallsCriticalThreshold()) {
                alerts.add(alert(AlertSeverity.CRITICAL, "CALLS", null,
                        "Dropped calls reached " + totalDropped,
                        "Check CVP, gateway, carrier, PG, and Router/Logger events for the same interval."));
            } else if (totalDropped >= pcceProperties.getOperations().getDroppedCallsWarningThreshold()) {
                alerts.add(alert(AlertSeverity.WARNING, "CALLS", null,
                        "Dropped calls reached " + totalDropped,
                        "Review call disposition breakdown and isolate affected skill groups."));
            }

            List<CallMetric> metrics = reportingService.callMetrics(from, to, null);
            BigDecimal serviceLevel = averageServiceLevel(metrics);
            if (serviceLevel != null
                    && serviceLevel.compareTo(BigDecimal.valueOf(pcceProperties.getOperations().getServiceLevelCriticalPct())) < 0) {
                alerts.add(alert(AlertSeverity.CRITICAL, "SERVICE_LEVEL", null,
                        "Service level is " + serviceLevel + "%",
                        "Check agent staffing, queue depth, routing scripts, and any component alerts."));
            } else if (serviceLevel != null
                    && serviceLevel.compareTo(BigDecimal.valueOf(pcceProperties.getOperations().getServiceLevelWarningPct())) < 0) {
                alerts.add(alert(AlertSeverity.WARNING, "SERVICE_LEVEL", null,
                        "Service level is " + serviceLevel + "%",
                        "Review interval trend and compare against forecasted staffing."));
            }
        } catch (Exception ex) {
            alerts.add(alert(AlertSeverity.WARNING, "DATA_COLLECTION", null,
                    "Could not collect reporting metrics: " + ex.getMessage(),
                    "Validate HDS/CVP reporting database credentials, schema, and connectivity."));
        }
        return alerts;
    }

    private List<ProductionReadinessItem> readiness(List<ComponentStatus> components) {
        List<ProductionReadinessItem> items = new ArrayList<>();
        long enabledComponents = pcceProperties.getComponents().stream().filter(PcceProperties.ComponentTarget::isEnabled).count();
        items.add(new ProductionReadinessItem(
                "Component Probes",
                enabledComponents > 0,
                enabledComponents + " component probes enabled",
                "Enable probes for every production PCCE/CVP/CUIC/Finesse component pair."));
        items.add(new ProductionReadinessItem(
                "Default Admin Password",
                pcceProperties.getSecurity().getUsers().stream().noneMatch(user -> user.getPassword() != null && user.getPassword().contains("change-me")),
                "One or more users may still use placeholder credentials",
                "Use bcrypt passwords from a vault or environment variables before production."));
        items.add(new ProductionReadinessItem(
                "Healthy Components",
                components.stream().noneMatch(component -> component.state() == ComponentState.DOWN),
                "Current component status has " + components.stream().filter(component -> component.state() == ComponentState.DOWN).count() + " DOWN components",
                "Resolve DOWN probes or suppress only during approved maintenance windows."));
        items.add(new ProductionReadinessItem(
                "Maintenance Mode",
                !pcceProperties.getOperations().isMaintenanceMode(),
                pcceProperties.getOperations().isMaintenanceMode() ? "Maintenance mode is enabled" : "Maintenance mode is disabled",
                "Keep maintenance mode disabled during normal banking production hours."));
        return items;
    }

    private BigDecimal averageServiceLevel(List<CallMetric> metrics) {
        List<BigDecimal> values = metrics.stream()
                .map(CallMetric::serviceLevelPct)
                .filter(value -> value != null)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private OperationalAlert alert(
            AlertSeverity severity,
            String category,
            String component,
            String message,
            String recommendedAction) {
        return new OperationalAlert(
                UUID.randomUUID().toString(),
                severity,
                AlertStatus.OPEN,
                category,
                component,
                message,
                recommendedAction,
                Instant.now());
    }
}
