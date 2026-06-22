package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.config.PcceProperties.ApiAction;
import com.example.pcceobservability.config.PcceProperties.ApiMonitor;
import com.example.pcceobservability.model.ApiActionResult;
import com.example.pcceobservability.model.ApiActionView;
import com.example.pcceobservability.model.ApiCapability;
import com.example.pcceobservability.model.ApiFunctionView;
import com.example.pcceobservability.model.ApiMonitorStatus;
import com.example.pcceobservability.model.ComponentState;
import com.example.pcceobservability.model.RtmtCapability;
import com.example.pcceobservability.model.SpogCapability;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PcceApiMonitoringService {

    private final PcceProperties pcceProperties;

    public PcceApiMonitoringService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
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
                new ApiFunctionView("Call Configuration and Management", "List dialed numbers", "GET", "/unifiedconfig/config/dialednumber", "Read DN configuration used by routing scripts"),
                new ApiFunctionView("Call Configuration and Management", "List call types", "GET", "/unifiedconfig/config/calltype", "Read call type configuration for CUIC/report mapping"),
                new ApiFunctionView("Call Configuration and Management", "Expanded call variables", "GET", "/unifiedconfig/config/expandedcallvariable", "Validate ECC variable configuration availability"),
                new ApiFunctionView("Outbound Option", "Campaigns", "GET", "/unifiedconfig/config/campaign", "Read outbound campaign configuration where Outbound Option is enabled"),
                new ApiFunctionView("Outbound Option", "Do Not Call import rules", "GET", "/unifiedconfig/config/dncimportrule", "Validate DNC import configuration where Outbound Option is enabled"),
                new ApiFunctionView("System Configuration", "Business hours", "GET", "/unifiedconfig/config/businesshour", "Read business hour configuration"),
                new ApiFunctionView("System Configuration", "Deployment information", "GET", "/unifiedconfig/config/deploymenttype", "Validate deployment-level API availability"),
                new ApiFunctionView("System Configuration", "CVP Reporting Server", "GET", "/unifiedconfig/config/cvpreportingserver", "Validate CVP Reporting Server configuration visibility"));
    }

    public List<ApiActionView> actions() {
        PcceProperties.PcceApi api = pcceProperties.getPcceApi();
        if (api == null || api.getActions() == null) {
            return List.of();
        }
        return api.getActions().stream()
                .map(this::toView)
                .toList();
    }

    public ApiActionResult execute(String id, String body) {
        PcceProperties.PcceApi api = pcceProperties.getPcceApi();
        ApiAction action = api.getActions().stream()
                .filter(candidate -> id.equalsIgnoreCase(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PCCE API action: " + id));
        if (!api.isEnabled() || !action.isEnabled()) {
            throw new IllegalArgumentException("PCCE API action is disabled: " + id);
        }
        String target = target(api, action.getPath());
        long start = System.nanoTime();
        try {
            HttpURLConnection connection = open(api, action.getMethod(), target, action.getContentType());
            if (hasBody(action.getMethod()) && body != null) {
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int statusCode = connection.getResponseCode();
            String responseBody = readBody(connection, statusCode);
            return new ApiActionResult(id, action.getMethod(), target, statusCode, elapsedMs(start), responseBody, Instant.now());
        } catch (IOException ex) {
            throw new IllegalStateException("PCCE API action failed: " + ex.getMessage(), ex);
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
        HttpURLConnection connection = open(api, monitor.getMethod(), target, "application/json");
        return connection.getResponseCode();
    }

    private HttpURLConnection open(
            PcceProperties.PcceApi api,
            String method,
            String target,
            String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (api.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
        }
        connection.setConnectTimeout((int) api.getTimeout().toMillis());
        connection.setReadTimeout((int) api.getTimeout().toMillis());
        connection.setRequestMethod(StringUtils.hasText(method) ? method : "GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", StringUtils.hasText(contentType) ? contentType : "application/json");
        if (StringUtils.hasText(api.getUsername()) && api.getPassword() != null) {
            String token = Base64.getEncoder().encodeToString((api.getUsername() + ":" + api.getPassword())
                    .getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + token);
        }
        return connection;
    }

    private void trustAll(HttpsURLConnection connection) throws IOException {
        try {
            TrustManager[] trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new java.security.SecureRandom());
            HostnameVerifier verifier = (hostname, session) -> true;
            connection.setSSLSocketFactory(context.getSocketFactory());
            connection.setHostnameVerifier(verifier);
        } catch (Exception ex) {
            throw new IOException("Unable to initialize relaxed TLS for PCCE API monitor", ex);
        }
    }

    private String target(PcceProperties.PcceApi api, ApiMonitor monitor) {
        if (monitor == null || !StringUtils.hasText(monitor.getPath())) {
            return "";
        }
        return target(api, monitor.getPath());
    }

    private String target(PcceProperties.PcceApi api, String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!StringUtils.hasText(api.getBaseUrl())) {
            return path;
        }
        return URI.create(api.getBaseUrl().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", ""))
                .toString();
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
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
