package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.config.PcceProperties.ApiMonitor;
import com.example.pcceobservability.model.ApiCapability;
import com.example.pcceobservability.model.ApiMonitorStatus;
import com.example.pcceobservability.model.ComponentState;
import java.io.IOException;
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
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (api.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
        }
        connection.setConnectTimeout((int) api.getTimeout().toMillis());
        connection.setReadTimeout((int) api.getTimeout().toMillis());
        connection.setRequestMethod(StringUtils.hasText(monitor.getMethod()) ? monitor.getMethod() : "GET");
        connection.setRequestProperty("Accept", "application/json");
        if (StringUtils.hasText(api.getUsername()) && api.getPassword() != null) {
            String token = Base64.getEncoder().encodeToString((api.getUsername() + ":" + api.getPassword())
                    .getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + token);
        }
        return connection.getResponseCode();
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
        if (monitor.getPath().startsWith("http://") || monitor.getPath().startsWith("https://")) {
            return monitor.getPath();
        }
        if (!StringUtils.hasText(api.getBaseUrl())) {
            return monitor.getPath();
        }
        return URI.create(api.getBaseUrl().replaceAll("/+$", "") + "/" + monitor.getPath().replaceAll("^/+", ""))
                .toString();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
