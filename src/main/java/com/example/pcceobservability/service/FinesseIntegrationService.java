package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.FinesseEndpointResult;
import com.example.pcceobservability.model.IntegrationCapability;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinesseIntegrationService {

    private final PcceProperties pcceProperties;
    private final JdbcTemplate awJdbcTemplate;

    public FinesseIntegrationService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
    }

    public List<IntegrationCapability> capabilities() {
        return List.of(
                new IntegrationCapability("Finesse", "SystemInfo", "GET /finesse/api/SystemInfo", status(), "Validate Finesse desktop API and service health"),
                new IntegrationCapability("Finesse", "User directory", "GET /finesse/api/Users", status(), "Discover Finesse-visible users from Finesse and AW login IDs"),
                new IntegrationCapability("Finesse", "Live agent state", "GET /finesse/api/User/{id}", status(), "Use AW LoginName/Finesse login ID, not only numeric SkillTargetID"),
                new IntegrationCapability("Finesse", "Agent dialogs", "GET /finesse/api/User/{id}/Dialogs", status(), "Track active calls, held calls, participants, and dialog lifecycle"),
                new IntegrationCapability("Finesse", "Teams", "GET /finesse/api/Team/{id}", status(), "Configure pcce.finesse.team-ids for supervisor/team monitoring"),
                new IntegrationCapability("Finesse", "Queues", "GET /finesse/api/Queue/{id}", status(), "Use when exposed by your deployment; otherwise keep queue KPIs from AW/HDS/CUIC"));
    }

    public List<FinesseEndpointResult> system() {
        return List.of(request("SystemInfo", "/finesse/api/SystemInfo"));
    }

    public List<FinesseEndpointResult> agents() {
        List<FinesseEndpointResult> results = new ArrayList<>();
        results.add(request("Users Directory", "/finesse/api/Users"));
        results.addAll(configuredUserIds().stream()
                .limit(25)
                .map(userId -> request("User " + userId, "/finesse/api/User/" + encodePath(userId)))
                .toList());
        return results;
    }

    public List<FinesseEndpointResult> dialogs() {
        return configuredUserIds().stream()
                .limit(50)
                .map(userId -> request("Dialogs " + userId, "/finesse/api/User/" + encodePath(userId) + "/Dialogs"))
                .toList();
    }

    public List<FinesseEndpointResult> teams() {
        List<String> teamIds = filtered(pcceProperties.getFinesse().getTeamIds());
        if (teamIds.isEmpty()) {
            return List.of(request("Teams Directory", "/finesse/api/Teams"));
        }
        return teamIds.stream()
                .map(teamId -> request("Team " + teamId, "/finesse/api/Team/" + encodePath(teamId)))
                .toList();
    }

    public List<FinesseEndpointResult> queues() {
        List<String> queueIds = filtered(pcceProperties.getFinesse().getQueueIds());
        if (queueIds.isEmpty()) {
            return List.of(request("Queues Directory", "/finesse/api/Queues"));
        }
        return queueIds.stream()
                .map(queueId -> request("Queue " + queueId, "/finesse/api/Queue/" + encodePath(queueId)))
                .toList();
    }

    private FinesseEndpointResult request(String name, String path) {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        String target = target(path);
        if (finesse == null || !finesse.isEnabled()) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start),
                    "Finesse integration disabled. Set pcce.finesse.enabled=true and configure credentials.", checkedAt);
        }
        if (!StringUtils.hasText(finesse.getBaseUrl())) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start),
                    "pcce.finesse.base-url is required.", checkedAt);
        }
        try {
            HttpURLConnection connection = open(finesse, target);
            int statusCode = connection.getResponseCode();
            return new FinesseEndpointResult(name, "GET", target, statusCode, elapsedMs(start),
                    readBody(connection, statusCode), checkedAt);
        } catch (IOException ex) {
            return new FinesseEndpointResult(name, "GET", target, 0, elapsedMs(start), ex.getMessage(), checkedAt);
        }
    }

    private HttpURLConnection open(PcceProperties.Finesse finesse, String target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (finesse.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
        }
        int timeoutMs = (int) (finesse.getTimeout() == null ? 10000 : finesse.getTimeout().toMillis());
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/xml, application/json;q=0.9, text/xml;q=0.8");
        if (StringUtils.hasText(finesse.getUsername()) && finesse.getPassword() != null) {
            String token = Base64.getEncoder().encodeToString((finesse.getUsername() + ":" + finesse.getPassword())
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
            throw new IOException("Unable to initialize relaxed TLS for Finesse API", ex);
        }
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (statusCode == 401 || statusCode == 403 || body.toLowerCase().contains("access denied")) {
                return "Access denied by Finesse. Verify username/password, user role, API permissions, and that the endpoint is enabled on the Finesse node.";
            }
            return body.length() > 5000 ? body.substring(0, 5000) + "\n...truncated..." : body;
        }
    }

    private List<String> configuredUserIds() {
        Set<String> userIds = new LinkedHashSet<>(filtered(pcceProperties.getFinesse().getUserIds()));
        userIds.addAll(awFinesseUserIds());
        for (PcceProperties.AppUser user : pcceProperties.getSecurity().getUsers()) {
            if (StringUtils.hasText(user.getAgentId())) {
                userIds.add(user.getAgentId().trim());
            }
        }
        return new ArrayList<>(userIds);
    }

    private List<String> awFinesseUserIds() {
        try {
            return awJdbcTemplate.query("""
                    SELECT TOP 500
                        COALESCE(p.LoginName, CAST(a.SkillTargetID AS varchar(50))) AS user_id
                    FROM t_Agent a
                    LEFT JOIN t_Person p ON p.PersonID = a.PersonID
                    WHERE COALESCE(p.LoginName, CAST(a.SkillTargetID AS varchar(50))) IS NOT NULL
                    ORDER BY user_id
                    """, (rs, rowNum) -> rs.getString("user_id")).stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> filtered(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(value -> !"null".equalsIgnoreCase(value))
                .toList();
    }

    private String target(String path) {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        if (finesse == null || !StringUtils.hasText(finesse.getBaseUrl())) {
            return path;
        }
        return URI.create(finesse.getBaseUrl().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "")).toString();
    }

    private String encodePath(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String status() {
        PcceProperties.Finesse finesse = pcceProperties.getFinesse();
        return finesse != null && finesse.isEnabled() ? "Configured" : "Config required";
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
