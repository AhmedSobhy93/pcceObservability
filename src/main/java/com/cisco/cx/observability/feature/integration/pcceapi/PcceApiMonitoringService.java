package com.cisco.cx.observability.feature.integration.pcceapi;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.ApiAction;
import com.cisco.cx.observability.config.PcceProperties.ApiMonitor;
import com.cisco.cx.observability.feature.operations.domain.RtmtCapability;
import com.cisco.cx.observability.feature.operations.domain.SpogCapability;
import com.cisco.cx.observability.shared.domain.ComponentState;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PcceApiMonitoringService {

    private final PcceProperties pcceProperties;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;

    public PcceApiMonitoringService(
            PcceProperties pcceProperties,
            SSLSocketFactory sslSocketFactory,
            HostnameVerifier hostnameVerifier) {
        this.pcceProperties = pcceProperties;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    public List<ApiCapability> capabilities() {
        return List.of(
                new ApiCapability("User Configuration and Management", "Create users by role, list users by role, view/update users, delete users, manage agent desk settings"),
                new ApiCapability("Skill Group Management", "View and manage skill group configuration used for routing and reporting"),
                new ApiCapability("Team Configuration and Management", "Create/list/view/update/delete agent teams"),
                new ApiCapability("Outbound Option", "Manage campaigns, DNC import rules, contacts, callbacks, and campaign runtime status"),
                new ApiCapability("Call Configuration and Management", "Manage dialed numbers, call types, expanded call variables, and bulk configuration"),
                new ApiCapability("Agents, Skills, Attributes, and Teams", "Manage agent associations used by operations and workforce teams"),
                new ApiCapability("System Configuration", "Manage business hours, timezone, CVP Reporting Server hostname, remote data centers, deployment type, and capacity"));
    }

    public List<ApiFunctionView> functions() {
        return List.of(
                new ApiFunctionView("User Configuration and Management", "List users", "GET", "/unifiedconfig/config/user", "Read configured users and validate user-management API reachability"),
                new ApiFunctionView("User Configuration and Management", "User by role", "GET", "/unifiedconfig/config/user?role={role}", "Filter users by configured PCCE role"),
                new ApiFunctionView("Team Configuration and Management", "List agent teams", "GET", "/unifiedconfig/config/agentteam", "Read agent team configuration for workforce visibility"),
                new ApiFunctionView("Skill Group Management", "List skill groups", "GET", "/unifiedconfig/config/skillgroup", "Read skill groups used for routing/reporting alignment"),
                new ApiFunctionView("Agents, Skills, Attributes, and Teams", "Agent by ID", "GET", "/unifiedconfig/config/agent/{id}", "Inspect one configured agent and validate ID mapping"),
                new ApiFunctionView("Agents, Skills, Attributes, and Teams", "List attributes", "GET", "/unifiedconfig/config/attribute", "Read agent/routing attributes where configured"),
                new ApiFunctionView("Call Configuration and Management", "List dialed numbers", "GET", "/unifiedconfig/config/dialednumber", "Read DN configuration used by routing scripts"),
                new ApiFunctionView("Call Configuration and Management", "List call types", "GET", "/unifiedconfig/config/calltype", "Read call type configuration for CUIC/report mapping"),
                new ApiFunctionView("Call Configuration and Management", "List labels", "GET", "/unifiedconfig/config/label", "Read routing labels and targets"),
                new ApiFunctionView("Call Configuration and Management", "Expanded call variables", "GET", "/unifiedconfig/config/expandedcallvariable", "Validate ECC variable configuration availability"),
                new ApiFunctionView("Outbound Option", "Campaigns", "GET", "/unifiedconfig/config/campaign", "Read outbound campaign configuration where Outbound Option is enabled"),
                new ApiFunctionView("Outbound Option", "Do Not Call import rules", "GET", "/unifiedconfig/config/dncimportrule", "Validate DNC import configuration where Outbound Option is enabled"),
                new ApiFunctionView("System Configuration", "Business hours", "GET", "/unifiedconfig/config/businesshour", "Read business hour configuration"),
                new ApiFunctionView("System Configuration", "Capacity info", "GET", "/unifiedconfig/config/capacityinfo", "Read deployment capacity information where exposed"),
                new ApiFunctionView("System Configuration", "Deployment information", "GET", "/unifiedconfig/config/deploymenttype", "Validate deployment-level API availability"),
                new ApiFunctionView("System Configuration", "Machine inventory", "GET", "/unifiedconfig/config/machineinventory?q=datacenter:{name}&resultsPerPage=100", "Read SPOG inventory: machine type, hostname, version, network addresses, services, and service ports"),
                new ApiFunctionView("System Configuration", "CVP Reporting Server", "GET", "/unifiedconfig/config/cvpreportingserver", "Validate CVP Reporting Server configuration visibility"));
    }

    public List<ApiActionView> actions() {
        PcceProperties.PcceApi api = pcceProperties.getPcceApi();
        if (api == null || api.getActions() == null) {
            return List.of();
        }
        return mergedActions(api).stream()
                .map(this::toView)
                .toList();
    }

    public ApiActionResult execute(String id, String body) {
        return execute(id, body, Map.of(), Map.of());
    }

    public ApiActionResult execute(String id, String body, Map<String, String> pathParams, Map<String, String> queryParams) {
        PcceProperties.PcceApi api = pcceProperties.getPcceApi();
        ApiAction action = mergedActions(api).stream()
                .filter(candidate -> id.equalsIgnoreCase(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PCCE API action: " + id));
        if (!api.isEnabled() || !action.isEnabled()) {
            throw new IllegalArgumentException("PCCE API action is disabled: " + id);
        }
        String target = target(api, applyPathParams(action.getPath(), pathParams), queryParams);
        long start = System.nanoTime();
        try {
            boolean requestHasBody = hasBody(action.getMethod()) && body != null && !body.isBlank();
            HttpURLConnection connection = open(api, action.getMethod(), target, action.getContentType());
            if (requestHasBody) {
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int statusCode = connection.getResponseCode();
            String responseBody = readBody(connection, statusCode);
            return new ApiActionResult(id, action.getMethod(), target, statusCode, elapsedMs(start), responseBody, Instant.now());
        } catch (IOException ex) {
            return new ApiActionResult(id, action.getMethod(), target, 0, elapsedMs(start),
                    "PCCE API action failed before HTTP response: " + ex.getMessage()
                            + ". Check base URL, DNS/host, certificate trust, credentials, and IIS/API exposure.",
                    Instant.now());
        }
    }

    public List<RtmtCapability> rtmtCapabilities() {
        return List.of(
                new RtmtCapability("Infrastructure", "CPU, memory, disk, network, process and service state", "Windows perf counters, SNMP/WMI, or enterprise agent"),
                new RtmtCapability("CUCM", "Call processing, device registration, SIP trunks, CTI Manager, DB replication", "CUCM RTMT counters/SNMP/syslog"),
                new RtmtCapability("CVP", "Call Server, VXML Server, reporting connectivity, SIP service, JVM/Tomcat health", "CVP diagnostics, SNMP/JMX, logs"),
                new RtmtCapability("Finesse", "Desktop service, notification service, Tomcat/JVM, agent desktop reachability", "HTTPS checks, JVM counters, logs"),
                new RtmtCapability("CUIC", "Reporting web app, datasource status, scheduler and report execution health", "HTTPS checks, datasource probes, logs"),
                new RtmtCapability("UCCE/PCCE", "Router, Logger, PG, CTI Server, AW/HDS database status", "TCP/JDBC probes, Windows services, SQL counters"));
    }

    public List<SpogCapability> spogCapabilities() {
        return List.of(
                new SpogCapability("Executive View", "Single health score across PCCE, CVP, CUIC, Finesse, VVB, gateway and databases", "Dashboard aggregation"),
                new SpogCapability("Operations View", "Live alerts, component state, query latency, API surveillance and health history", "Existing app telemetry"),
                new SpogCapability("Support View", "Run approved PCCE API read actions and validate config objects", "PCCE API Console"),
                new SpogCapability("Business View", "Call volume, handled calls, abandon/dropped definitions, service level and agent/team activity", "HDS/CUIC-aligned reporting"),
                new SpogCapability("Audit View", "Admin changes, role permission changes, probe changes and maintenance mode", "Audit log and app logs"),
                new SpogCapability("Notification View", "Webhook alerts to Teams/SIEM/ITSM", "Configured webhook notifications"));
    }

    public List<ApiMonitorStatus> status() {
        PcceProperties.PcceApi api = pcceProperties.getPcceApi();
        if (api == null || api.getMonitors() == null) {
            return List.of();
        }
        return api.getMonitors().stream()
                .map(monitor -> probe(api, monitor))
                .toList();
    }

    private ApiMonitorStatus probe(PcceProperties.PcceApi api, ApiMonitor monitor) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        String target = target(api, monitor);
        if (!api.isEnabled() || !monitor.isEnabled()) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DISABLED, 0, 0, "PCCE API monitor disabled in configuration", checkedAt);
        }
        if (!StringUtils.hasText(api.getBaseUrl())) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DOWN, 0, elapsedMs(start), "pcce.pcce-api.base-url is required", checkedAt);
        }

        try {
            int statusCode = request(api, monitor, target);
            boolean expected = statusCode >= monitor.getExpectedStatusMin() && statusCode <= monitor.getExpectedStatusMax();
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    expected ? ComponentState.UP : ComponentState.DOWN, statusCode, elapsedMs(start),
                    expected ? "OK" : "Unexpected HTTP status " + statusCode, checkedAt);
        } catch (Exception ex) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DOWN, 0, elapsedMs(start), ex.getMessage(), checkedAt);
        }
    }

    private int request(PcceProperties.PcceApi api, ApiMonitor monitor, String target) throws IOException {
        HttpURLConnection connection = open(api, monitor.getMethod(), target, null);
        return connection.getResponseCode();
    }

    private HttpURLConnection open(
            PcceProperties.PcceApi api,
            String method,
            String target,
            String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
            httpsConnection.setHostnameVerifier(hostnameVerifier);
        }
        connection.setConnectTimeout((int) api.getTimeout().toMillis());
        connection.setReadTimeout((int) api.getTimeout().toMillis());
        connection.setRequestMethod(StringUtils.hasText(method) ? method : "GET");
        connection.setRequestProperty("Accept", "application/json, application/xml, text/xml, text/plain, */*; q=0.1");
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        if (StringUtils.hasText(contentType)) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        applyAuthentication(api, connection);
        return connection;
    }

    private void applyAuthentication(PcceProperties.PcceApi api, HttpURLConnection connection) {
        switch (api.getAuthMode() == null ? PcceProperties.ApiAuthMode.BASIC : api.getAuthMode()) {
            case NONE -> {
            }
            case BASIC -> {
                if (StringUtils.hasText(api.getUsername()) && api.getPassword() != null) {
                    String token = Base64.getEncoder().encodeToString((api.getUsername() + ":" + api.getPassword())
                            .getBytes(StandardCharsets.UTF_8));
                    connection.setRequestProperty("Authorization", "Basic " + token);
                }
            }
            case BEARER -> {
                if (StringUtils.hasText(api.getBearerToken())) {
                    connection.setRequestProperty("Authorization", "Bearer " + api.getBearerToken());
                }
            }
            case API_KEY -> {
                if (StringUtils.hasText(api.getApiKeyHeaderName()) && StringUtils.hasText(api.getApiKey())) {
                    connection.setRequestProperty(api.getApiKeyHeaderName(), api.getApiKey());
                }
            }
        }
    }

    private String target(PcceProperties.PcceApi api, ApiMonitor monitor) {
        if (monitor == null || !StringUtils.hasText(monitor.getPath())) {
            return "";
        }
        return target(api, monitor.getPath());
    }

    private String target(PcceProperties.PcceApi api, String path) {
        return target(api, path, Map.of());
    }

    private String target(PcceProperties.PcceApi api, String path, Map<String, String> queryParams) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return appendQuery(path, queryParams);
        }
        if (!StringUtils.hasText(api.getBaseUrl())) {
            return appendQuery(path, queryParams);
        }
        return appendQuery(URI.create(normalizeBaseUrl(api.getBaseUrl()).replaceAll("/+$", "") + "/" + path.replaceAll("^/+", ""))
                .toString(), queryParams);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (!StringUtils.hasText(value) || value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    private String applyPathParams(String path, Map<String, String> pathParams) {
        String resolved = path;
        if (pathParams == null) {
            return resolved;
        }
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
        }
        if (resolved.matches(".*\\{[^/]+}.*")) {
            throw new IllegalArgumentException("Missing path params for API path: " + resolved);
        }
        return resolved;
    }

    private List<ApiAction> mergedActions(PcceProperties.PcceApi api) {
        Map<String, ApiAction> actions = new LinkedHashMap<>();
        if (api.getActions() != null) {
            api.getActions().forEach(action -> actions.put(action.getId().toLowerCase(), action));
        }
        defaultReadActions().forEach(action -> actions.put(action.getId().toLowerCase(), action));
        return List.copyOf(actions.values());
    }

    private List<ApiAction> defaultReadActions() {
        return List.of(
                action("users.list", "User Configuration and Management", "List users", "GET", "/unifiedconfig/config/user"),
                action("teams.list", "Team Configuration and Management", "List agent teams", "GET", "/unifiedconfig/config/agentteam"),
                action("skills.list", "Skill Group Management", "List skill groups", "GET", "/unifiedconfig/config/skillgroup"),
                action("agents.list", "Agents, Skills, Attributes, and Teams", "List agents", "GET", "/unifiedconfig/config/agent"),
                action("callTypes.list", "Call Configuration and Management", "List call types", "GET", "/unifiedconfig/config/calltype"),
                action("machineInventory.list", "System Configuration", "List machine inventory", "GET", "/unifiedconfig/config/machineinventory"));
    }

    private ApiAction action(String id, String category, String name, String method, String path) {
        ApiAction action = new ApiAction();
        action.setId(id);
        action.setCategory(category);
        action.setName(name);
        action.setMethod(method);
        action.setPath(path);
        action.setAdminOnly(false);
        action.setEnabled(true);
        action.setContentType("application/xml");
        return action;
    }

    private String appendQuery(String target, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return target;
        }
        StringBuilder builder = new StringBuilder(target);
        builder.append(target.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            if (!first) {
                builder.append("&");
            }
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private ApiActionView toView(ApiAction action) {
        return new ApiActionView(
                action.getId(),
                action.getCategory(),
                action.getName(),
                action.isEnabled(),
                action.isAdminOnly(),
                action.getMethod(),
                action.getPath(),
                action.getContentType());
    }

    private boolean hasBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (statusCode == 401 || statusCode == 403 || body.toLowerCase().contains("access denied")) {
                return "Access denied by PCCE/IIS. Check pcce.pcce-api.auth-mode, username format, password/token, API user permissions, IIS authentication, endpoint path, and that this node exposes the PCCE REST API.";
            }
            return body;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
