package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.AppRole;
import com.cisco.cx.observability.config.PcceProperties.AppUser;
import com.cisco.cx.observability.config.PcceProperties.ComponentName;
import com.cisco.cx.observability.model.ManagedRoleView;
import com.cisco.cx.observability.model.ManagedUserView;
import com.cisco.cx.observability.model.NotificationSettingsView;
import com.cisco.cx.observability.model.ServerLogTarget;
import com.cisco.cx.observability.model.UpdateComponentRequest;
import com.cisco.cx.observability.model.UpdateNotificationSettingsRequest;
import com.cisco.cx.observability.model.UpdateRolePermissionsRequest;
import com.cisco.cx.observability.model.UpdateServerLogTargetRequest;
import com.cisco.cx.observability.model.UpsertUserRequest;
import com.cisco.cx.observability.security.access.PermissionCatalog;
import com.cisco.cx.observability.service.AuditService;
import com.cisco.cx.observability.service.SupportCapabilityService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
public class AdminController {

    private final PcceProperties pcceProperties;
    private final AuditService auditService;
    private final SupportCapabilityService supportCapabilityService;

    public AdminController(
            PcceProperties pcceProperties,
            AuditService auditService,
            SupportCapabilityService supportCapabilityService) {
        this.pcceProperties = pcceProperties;
        this.auditService = auditService;
        this.supportCapabilityService = supportCapabilityService;
    }

    @GetMapping("/users")
    public List<ManagedUserView> users() {
        return pcceProperties.getSecurity().getUsers().stream()
                .map(this::toView)
                .toList();
    }

    @PutMapping("/users/{username}")
    public ManagedUserView upsertUser(@PathVariable String username, @RequestBody UpsertUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        List<AppUser> users = mutableUsers();
        AppUser user = users.stream()
                .filter(candidate -> candidate.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElseGet(() -> {
                    if (!StringUtils.hasText(request.password())) {
                        throw new IllegalArgumentException("password is required when creating a user");
                    }
                    AppUser created = new AppUser();
                    created.setUsername(username);
                    users.add(created);
                    pcceProperties.getSecurity().setUsers(users);
                    return created;
                });

        if (StringUtils.hasText(request.password())) {
            user.setPassword(request.password());
        }
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.agentId() != null) {
            user.setAgentId(request.agentId());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.allowedTeams() != null) {
            user.setAllowedTeams(request.allowedTeams());
        }
        if (request.roles() != null) {
            user.setRoles(request.roles());
        }
        if (request.extraPermissions() != null) {
            user.setExtraPermissions(request.extraPermissions());
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", user.isEnabled());
        details.put("roles", user.getRoles());
        details.put("allowedTeams", user.getAllowedTeams());
        auditService.record("UPSERT_USER", username, details);
        return toView(user);
    }

    @GetMapping("/roles")
    public List<ManagedRoleView> roles() {
        return Arrays.stream(AppRole.values())
                .map(role -> new ManagedRoleView(
                        role,
                        rolePermissions(role)))
                .toList();
    }

    @PutMapping("/roles/{role}")
    public ManagedRoleView updateRole(
            @PathVariable AppRole role,
            @RequestBody UpdateRolePermissionsRequest request) {
        if (request == null || request.permissions() == null) {
            throw new IllegalArgumentException("permissions are required");
        }
        if (pcceProperties.getSecurity().getRolePermissions() == null) {
            pcceProperties.getSecurity().setRolePermissions(new java.util.EnumMap<>(AppRole.class));
        }
        pcceProperties.getSecurity().getRolePermissions().put(role, List.copyOf(request.permissions()));
        auditService.record("UPDATE_ROLE_PERMISSIONS", role.name(), Map.of(
                "permissions", request.permissions()));
        return new ManagedRoleView(role, pcceProperties.getSecurity().getRolePermissions().get(role));
    }

    @GetMapping("/components")
    public List<PcceProperties.ComponentTarget> components() {
        return pcceProperties.getComponents();
    }

    @PutMapping("/components/{name}")
    public PcceProperties.ComponentTarget updateComponent(
            @PathVariable ComponentName name,
            @RequestBody UpdateComponentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PcceProperties.ComponentTarget target = pcceProperties.getComponents().stream()
                .filter(candidate -> candidate.getName() == name)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown component: " + name));

        if (request.enabled() != null) {
            target.setEnabled(request.enabled());
        }
        if (request.displayName() != null) {
            target.setDisplayName(request.displayName());
        }
        if (request.side() != null) {
            target.setSide(request.side());
        }
        if (request.site() != null) {
            target.setSite(request.site());
        }
        if (request.tier() != null) {
            target.setTier(request.tier());
        }
        if (request.probe() != null) {
            target.setProbe(request.probe());
        }
        if (request.host() != null) {
            target.setHost(request.host());
        }
        if (request.port() != null) {
            target.setPort(request.port());
        }
        if (request.url() != null) {
            target.setUrl(request.url());
        }
        if (request.timeoutSeconds() != null) {
            target.setTimeout(Duration.ofSeconds(request.timeoutSeconds()));
        }
        if (request.trustAllCertificates() != null) {
            target.setTrustAllCertificates(request.trustAllCertificates());
        }
        if (request.expectedStatusMin() != null) {
            target.setExpectedStatusMin(request.expectedStatusMin());
        }
        if (request.expectedStatusMax() != null) {
            target.setExpectedStatusMax(request.expectedStatusMax());
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", target.isEnabled());
        details.put("displayName", target.getDisplayName());
        details.put("side", target.getSide());
        details.put("site", target.getSite());
        details.put("tier", target.getTier());
        details.put("probe", target.getProbe());
        details.put("host", target.getHost());
        details.put("port", target.getPort());
        details.put("url", target.getUrl());
        details.put("timeout", target.getTimeout().toString());
        details.put("trustAllCertificates", target.isTrustAllCertificates());
        details.put("expectedStatusMin", target.getExpectedStatusMin());
        details.put("expectedStatusMax", target.getExpectedStatusMax());
        auditService.record("UPDATE_COMPONENT", name.name(), details);
        return target;
    }

    @GetMapping("/notifications")
    public NotificationSettingsView notifications() {
        PcceProperties.Notifications notifications = pcceProperties.getNotifications();
        return new NotificationSettingsView(
                notifications.isWebhookEnabled(),
                StringUtils.hasText(notifications.getWebhookUrl()) ? "configured" : "",
                notifications.isSmtpEnabled(),
                notifications.getSmtpFrom(),
                notifications.getSmtpRecipients(),
                notifications.getSmtpSubjectPrefix(),
                notifications.isSmsEnabled(),
                StringUtils.hasText(notifications.getSmsUrl()) ? "configured" : "",
                notifications.getSmsUserAgent(),
                notifications.getSmsRecipients(),
                notifications.getMinimumSeverity(),
                notifications.getSmsMinimumSeverity(),
                notifications.getSmsMaxAlertsPerAssessment());
    }

    @PutMapping("/notifications")
    public NotificationSettingsView updateNotifications(@RequestBody UpdateNotificationSettingsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        PcceProperties.Notifications notifications = pcceProperties.getNotifications();
        if (request.webhookEnabled() != null) {
            notifications.setWebhookEnabled(request.webhookEnabled());
        }
        if (request.smtpEnabled() != null) {
            notifications.setSmtpEnabled(request.smtpEnabled());
        }
        if (request.smtpFrom() != null) {
            notifications.setSmtpFrom(request.smtpFrom());
        }
        if (request.smtpRecipients() != null) {
            notifications.setSmtpRecipients(request.smtpRecipients());
        }
        if (request.smtpSubjectPrefix() != null) {
            notifications.setSmtpSubjectPrefix(request.smtpSubjectPrefix());
        }
        if (request.smsEnabled() != null) {
            notifications.setSmsEnabled(request.smsEnabled());
        }
        if (request.smsUrl() != null) {
            notifications.setSmsUrl(request.smsUrl());
        }
        if (request.smsAuthorization() != null) {
            notifications.setSmsAuthorization(request.smsAuthorization());
        }
        if (request.smsUserAgent() != null) {
            notifications.setSmsUserAgent(request.smsUserAgent());
        }
        if (request.smsRecipients() != null) {
            notifications.setSmsRecipients(request.smsRecipients());
        }
        if (request.minimumSeverity() != null) {
            notifications.setMinimumSeverity(request.minimumSeverity());
        }
        if (request.smsMinimumSeverity() != null) {
            notifications.setSmsMinimumSeverity(request.smsMinimumSeverity());
        }
        if (request.smsMaxAlertsPerAssessment() != null) {
            notifications.setSmsMaxAlertsPerAssessment(request.smsMaxAlertsPerAssessment());
        }
        auditService.record("UPDATE_NOTIFICATION_SETTINGS", "notifications", Map.of(
                "smtpEnabled", notifications.isSmtpEnabled(),
                "smsEnabled", notifications.isSmsEnabled(),
                "minimumSeverity", notifications.getMinimumSeverity(),
                "smsMinimumSeverity", notifications.getSmsMinimumSeverity()));
        return notifications();
    }

    @GetMapping("/server-log-targets")
    public List<ServerLogTarget> serverLogTargets() {
        return supportCapabilityService.serverLogTargets();
    }

    @PutMapping("/server-log-targets/{component}")
    public ServerLogTarget updateServerLogTarget(
            @PathVariable String component,
            @RequestBody UpdateServerLogTargetRequest request) {
        ServerLogTarget updated = supportCapabilityService.updateServerLogTarget(component, request);
        auditService.record("UPDATE_SERVER_LOG_TARGET", component, Map.of(
                "host", updated.host(),
                "enabled", updated.enabled(),
                "collectionMethod", updated.collectionMethod()));
        return updated;
    }

    private List<AppUser> mutableUsers() {
        return new ArrayList<>(pcceProperties.getSecurity().getUsers());
    }

    private ManagedUserView toView(AppUser user) {
        return new ManagedUserView(
                user.getUsername(),
                user.getDisplayName(),
                user.getAgentId(),
                user.isEnabled(),
                user.getAllowedTeams(),
                user.getRoles(),
                user.getExtraPermissions());
    }

    private List<PcceProperties.Permission> rolePermissions(AppRole role) {
        if (pcceProperties.getSecurity().getRolePermissions() == null) {
            return List.copyOf(PermissionCatalog.permissionsFor(role));
        }
        return pcceProperties.getSecurity().getRolePermissions()
                .getOrDefault(role, List.copyOf(PermissionCatalog.permissionsFor(role)));
    }
}
