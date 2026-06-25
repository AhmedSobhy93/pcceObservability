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
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CvpApiMonitoringService {

    private final PcceProperties pcceProperties;

    public CvpApiMonitoringService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    public List<ApiCapability> capabilities() {
        return List.of(
                new ApiCapability("VXML Application Management", "Deploy, update, inspect, and remove CVP/VXML applications through controlled API actions"),
                new ApiCapability("Media File Management", "Manage prompts and media assets used by IVR applications"),
                new ApiCapability("Operations Configuration", "Inspect and manage CVP Syslog and SNMP configuration used by monitoring and alerting"),
                new ApiCapability("Diagnostics and Health", "Probe CVP diagnostic endpoints and expose health state beside PCCE/Finesse/CUIC"),
                new ApiCapability("VVB and Voice Browser", "Visualize VVB/CVP service readiness and link CVP runtime checks with IVR journey data"));
    }

    public List<ApiFunctionView> functions() {
        return List.of(
                new ApiFunctionView("Diagnostics and Health", "CVP diagnostics", "GET", "/cvp/diag", "Lightweight reachability and diagnostic probe for CVP Call Server"),
                new ApiFunctionView("VXML Application Management", "List VXML applications", "GET", "/cvp/api/applications", "Inventory deployed CVP applications and compare with expected banking IVR apps"),
                new ApiFunctionView("VXML Application Management", "Application by name", "GET", "/cvp/api/applications/{name}", "Inspect one deployed application before rollout or rollback"),
                new ApiFunctionView("VXML Application Management", "Deploy/update application", "POST", "/cvp/api/applications/{name}", "Admin-only controlled deployment action with JSON/body support"),
                new ApiFunctionView("Media File Management", "List media files", "GET", "/cvp/api/media", "Check IVR prompt/media availability"),
                new ApiFunctionView("Media File Management", "Upload media metadata/file reference", "POST", "/cvp/api/media", "Admin-only media management action"),
                new ApiFunctionView("Operations Configuration", "Syslog config", "GET/PUT", "/cvp/api/syslog", "Validate or update CVP log forwarding configuration"),
                new ApiFunctionView("Operations Configuration", "SNMP config", "GET/PUT", "/cvp/api/snmp", "Validate or update CVP SNMP monitoring configuration"),
                new ApiFunctionView("VVB and Voice Browser", "VVB services", "GET", "/cvp/api/vvb", "Check VVB service inventory and readiness where exposed"));
    }

    public List<ApiActionView> actions() {
        PcceProperties.CvpApi api = pcceProperties.getCvpApi();
        if (api == null || api.getActions() == null) {
            return List.of();
        }
        return api.getActions().stream().map(this::toView).toList();
    }

    public List<ApiMonitorStatus> status() {
        PcceProperties.CvpApi api = pcceProperties.getCvpApi();
        if (api == null || api.getMonitors() == null) {
            return List.of();
        }
        return api.getMonitors().stream().map(monitor -> probe(api, monitor)).toList();
    }

    public ApiActionResult execute(String id, String body, Map<String, String> pathParams, Map<String, String> queryParams) {
        PcceProperties.CvpApi api = pcceProperties.getCvpApi();
        ApiAction action = api.getActions().stream()
                .filter(candidate -> id.equalsIgnoreCase(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown CVP API action: " + id));
        if (!api.isEnabled() || !action.isEnabled()) {
            throw new IllegalArgumentException("CVP API action is disabled: " + id);
        }
        String target = target(api, applyPathParams(action.getPath(), pathParams), queryParams);
        long start = System.nanoTime();
        try {
            boolean requestHasBody = hasBody(action.getMethod()) && body != null && !body.isBlank();
            HttpURLConnection connection = open(api, action.getMethod(), target, requestHasBody ? action.getContentType() : null);
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
            throw new IllegalStateException("CVP API action failed: " + ex.getMessage(), ex);
        }
    }

    private ApiMonitorStatus probe(PcceProperties.CvpApi api, ApiMonitor monitor) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        String target = target(api, monitor.getPath());
        if (!api.isEnabled() || !monitor.isEnabled()) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DISABLED, 0, 0, "CVP API monitor disabled in configuration", checkedAt);
        }
        if (!StringUtils.hasText(api.getBaseUrl())) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DOWN, 0, elapsedMs(start), "pcce.cvp-api.base-url is required", checkedAt);
        }
        try {
            HttpURLConnection connection = open(api, monitor.getMethod(), target, null);
            int statusCode = connection.getResponseCode();
            boolean expected = statusCode >= monitor.getExpectedStatusMin() && statusCode <= monitor.getExpectedStatusMax();
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    expected ? ComponentState.UP : ComponentState.DOWN, statusCode, elapsedMs(start),
                    expected ? "OK" : "Unexpected HTTP status " + statusCode, checkedAt);
        } catch (Exception ex) {
            return new ApiMonitorStatus(monitor.getName(), monitor.getCategory(), monitor.getMethod(), target,
                    ComponentState.DOWN, 0, elapsedMs(start), ex.getMessage(), checkedAt);
        }
    }

    private HttpURLConnection open(PcceProperties.CvpApi api, String method, String target, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (api.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
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

    private void applyAuthentication(PcceProperties.CvpApi api, HttpURLConnection connection) {
        switch (api.getAuthMode() == null ? PcceProperties.ApiAuthMode.BASIC : api.getAuthMode()) {
            case NONE -> {
            }
            case BASIC -> {
                if (StringUtils.hasText(api.getUsername()) && api.getPassword() != null) {
                    String token = Base64.getEncoder().encodeToString((api.getUsername() + ":" + api.getPassword()).getBytes(StandardCharsets.UTF_8));
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
            throw new IOException("Unable to initialize relaxed TLS for CVP API monitor", ex);
        }
    }

    private String target(PcceProperties.CvpApi api, String path) {
        return target(api, path, Map.of());
    }

    private String target(PcceProperties.CvpApi api, String path, Map<String, String> queryParams) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String resolved = path.startsWith("http://") || path.startsWith("https://")
                ? path
                : StringUtils.hasText(api.getBaseUrl())
                        ? URI.create(api.getBaseUrl().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString()
                        : path;
        return appendQuery(resolved, queryParams);
    }

    private String applyPathParams(String path, Map<String, String> pathParams) {
        String resolved = path;
        if (pathParams == null) {
            return resolved;
        }
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", encode(entry.getValue()));
        }
        return resolved;
    }

    private String appendQuery(String target, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return target;
        }
        StringBuilder builder = new StringBuilder(target).append(target.contains("?") ? "&" : "?");
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
        return new ApiActionView(action.getId(), action.getCategory(), action.getName(), action.isEnabled(),
                action.isAdminOnly(), action.getMethod(), action.getPath(), action.getContentType());
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
                return "Access denied by CVP. Verify CVP API base URL, credentials, API exposure on this node, and user permissions.";
            }
            return body;
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
