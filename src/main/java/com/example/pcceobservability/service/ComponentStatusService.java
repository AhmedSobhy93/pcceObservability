package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.config.PcceProperties.ComponentTarget;
import com.example.pcceobservability.model.ComponentState;
import com.example.pcceobservability.model.ComponentStatus;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ComponentStatusService {

    private final PcceProperties pcceProperties;
    private final JdbcTemplate awJdbcTemplate;
    private final JdbcTemplate hdsJdbcTemplate;
    private final JdbcTemplate cvpReportingJdbcTemplate;
    private final ComponentHistoryService componentHistoryService;

    public ComponentStatusService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate,
            @Qualifier("hdsJdbcTemplate") JdbcTemplate hdsJdbcTemplate,
            @Qualifier("cvpReportingJdbcTemplate") JdbcTemplate cvpReportingJdbcTemplate,
            ComponentHistoryService componentHistoryService) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
        this.hdsJdbcTemplate = hdsJdbcTemplate;
        this.cvpReportingJdbcTemplate = cvpReportingJdbcTemplate;
        this.componentHistoryService = componentHistoryService;
    }

    public List<ComponentStatus> status() {
        return pcceProperties.getComponents().stream()
                .map(this::probe)
                .toList();
    }

    private ComponentStatus probe(ComponentTarget target) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        ComponentStatus status;
        if (!target.isEnabled()) {
            status = new ComponentStatus(target.getName(), ComponentState.DISABLED, target.getProbe(), describeTarget(target), 0,
                    "Probe disabled in configuration", checkedAt);
            componentHistoryService.record(status);
            return status;
        }

        try {
            switch (target.getProbe()) {
                case TCP -> tcpProbe(target);
                case HTTP -> httpProbe(target);
                case JDBC_AW -> jdbcProbe(awJdbcTemplate, "SELECT 1");
                case JDBC_HDS -> jdbcProbe(hdsJdbcTemplate, "SELECT 1");
                case JDBC_CVP_REPORTING -> jdbcProbe(cvpReportingJdbcTemplate, "SELECT FIRST 1 1 FROM systables");
            }
            status = new ComponentStatus(target.getName(), ComponentState.UP, target.getProbe(), describeTarget(target),
                    elapsedMs(start), "OK", checkedAt);
        } catch (Exception ex) {
            status = new ComponentStatus(target.getName(), ComponentState.DOWN, target.getProbe(), describeTarget(target),
                    elapsedMs(start), ex.getMessage(), checkedAt);
        }
        componentHistoryService.record(status);
        return status;
    }

    private void tcpProbe(ComponentTarget target) throws IOException {
        if (target.getHost() == null || target.getPort() <= 0) {
            throw new IllegalArgumentException("TCP probe requires host and port");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getHost(), target.getPort()), (int) target.getTimeout().toMillis());
        }
    }

    private void httpProbe(ComponentTarget target) throws IOException {
        if (target.getUrl() == null || target.getUrl().isBlank()) {
            throw new IllegalArgumentException("HTTP probe requires url");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(target.getUrl()).openConnection();
        if (target.isTrustAllCertificates() && connection instanceof HttpsURLConnection httpsConnection) {
            trustAll(httpsConnection);
        }
        connection.setConnectTimeout((int) target.getTimeout().toMillis());
        connection.setReadTimeout((int) target.getTimeout().toMillis());
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        int code = connection.getResponseCode();
        if (code < target.getExpectedStatusMin() || code > target.getExpectedStatusMax()) {
            throw new IOException("HTTP status " + code);
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
            throw new IOException("Unable to initialize relaxed TLS for component probe", ex);
        }
    }

    private void jdbcProbe(JdbcTemplate jdbcTemplate, String sql) {
        jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private String describeTarget(ComponentTarget target) {
        return switch (target.getProbe()) {
            case HTTP -> target.getUrl();
            case TCP -> target.getHost() + ":" + target.getPort();
            case JDBC_AW -> "AW database";
            case JDBC_HDS -> "HDS database";
            case JDBC_CVP_REPORTING -> "CVP reporting database";
        };
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
