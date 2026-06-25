package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.ExternalIntegrationStatus;
import com.example.pcceobservability.model.RealtimeDataView;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LiveDataIntegrationService {

    private final PcceProperties pcceProperties;
    private final JdbcTemplate awJdbcTemplate;

    public LiveDataIntegrationService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
    }

    public List<ExternalIntegrationStatus> tokenProbe() {
        PcceProperties.LiveData liveData = pcceProperties.getLiveData();
        if (liveData == null || !liveData.isEnabled()) {
            return List.of(status("Live Data Token", "disabled", "Live Data API",
                    "Set PCCE_LIVE_DATA_ENABLED=true and configure host/user/password from CUIC datasource."));
        }
        if (!StringUtils.hasText(liveData.getHost())) {
            return List.of(status("Live Data Token", "not configured", "Live Data API", "PCCE_LIVE_DATA_HOST is required."));
        }
        String target = tokenUrl(liveData);
        long started = System.nanoTime();
        try {
            ProbeResponse response = requestToken(liveData, target, "GET");
            if (response.statusCode() == 405 || response.statusCode() == 415) {
                response = requestToken(liveData, target, "POST");
            }
            String state = response.statusCode() >= 200 && response.statusCode() < 300 ? "up" : "warning";
            return List.of(
                    status("Live Data Token", state, target,
                            "HTTP " + response.statusCode() + " in " + elapsedMs(started) + "ms - " + abbreviate(response.body())),
                    status("Live Data WebSocket", "configured", websocketUrl(liveData),
                            "Use this WebSocket endpoint for Cisco stock realtime agent, skill group and call type streams."));
        } catch (Exception ex) {
            return List.of(
                    status("Live Data Token", "down", target, ex.getMessage()),
                    status("Live Data WebSocket", "configured", websocketUrl(liveData),
                            "WebSocket URL is configured, token probe failed."));
        }
    }

    public List<RealtimeDataView> realtimeSnapshots() {
        return List.of(
                snapshot("Agent Real Time", "AW t_Agent_Real_Time",
                        "Cisco stock agent realtime table; use for live agent state wallboards.",
                        """
                        SELECT TOP 25
                            DateTime,
                            SkillTargetID,
                            AgentState,
                            DateTimeLastStateChange
                        FROM t_Agent_Real_Time
                        ORDER BY DateTime DESC
                        """),
                snapshot("Skill Group Real Time", "AW t_Skill_Group_Real_Time",
                        "Cisco stock skill group realtime table; use for current queue/agent availability.",
                        """
                        SELECT TOP 25
                            DateTime,
                            SkillTargetID,
                            RouterCallsQueuedNow,
                            AgentsLoggedOn,
                            AgentsAvailable,
                            CallsHandled,
                            CallsAbandQ,
                            ServiceLevel
                        FROM t_Skill_Group_Real_Time
                        ORDER BY DateTime DESC
                        """),
                snapshot("Call Type Real Time", "AW t_Call_Type_Real_Time",
                        "Cisco stock call type realtime table; use for realtime business call flow by call type.",
                        """
                        SELECT TOP 25
                            DateTime,
                            CallTypeID,
                            CallsOffered,
                            CallsHandled,
                            CallsAbandQ,
                            ServiceLevel
                        FROM t_Call_Type_Real_Time
                        ORDER BY DateTime DESC
                        """));
    }

    private RealtimeDataView snapshot(String name, String source, String description, String sql) {
        try {
            List<Map<String, Object>> rows = awJdbcTemplate.queryForList(sql);
            return new RealtimeDataView(name, source, "up", description, rows, null, Instant.now());
        } catch (Exception ex) {
            return new RealtimeDataView(name, source, "warning", description, List.of(), ex.getMessage(), Instant.now());
        }
    }

    private ProbeResponse requestToken(PcceProperties.LiveData liveData, String target, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        if (liveData.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
        }
        int timeoutMs = (int) (liveData.getTimeout() == null ? 10000 : liveData.getTimeout().toMillis());
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*; q=0.1");
        if (StringUtils.hasText(liveData.getUsername()) && liveData.getPassword() != null) {
            String token = Base64.getEncoder().encodeToString((liveData.getUsername() + ":" + liveData.getPassword())
                    .getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + token);
        }
        int statusCode = connection.getResponseCode();
        return new ProbeResponse(statusCode, readBody(connection, statusCode));
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ExternalIntegrationStatus status(String name, String state, String target, String detail) {
        return new ExternalIntegrationStatus(name, "Cisco Live Data", state, target, detail);
    }

    private String tokenUrl(PcceProperties.LiveData liveData) {
        return URI.create("https://" + liveData.getHost() + ":" + liveData.getPort() + "/" + trimSlash(liveData.getTokenPath())).toString();
    }

    private String websocketUrl(PcceProperties.LiveData liveData) {
        return URI.create("wss://" + liveData.getHost() + ":" + liveData.getWebsocketPort()).toString();
    }

    private String trimSlash(String value) {
        return value == null ? "" : value.replaceAll("^/+", "");
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "empty response";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 240 ? normalized.substring(0, 240) + "..." : normalized;
    }

    private void trustAll(HttpsURLConnection connection) {
        try {
            TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
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
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            HostnameVerifier verifier = (hostname, session) -> true;
            connection.setHostnameVerifier(verifier);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to configure Live Data SSL trust override", ex);
        }
    }

    private record ProbeResponse(int statusCode, String body) {
    }
}
