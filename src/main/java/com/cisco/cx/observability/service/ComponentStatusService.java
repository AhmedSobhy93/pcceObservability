package com.cisco.cx.observability.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.config.PcceProperties.ComponentTarget;
import com.cisco.cx.observability.model.ComponentStatus;
import com.cisco.cx.observability.shared.domain.ComponentState;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
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
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicReference<Double>> componentStatusGauges = new ConcurrentHashMap<>();

    public ComponentStatusService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate,
            @Qualifier("hdsJdbcTemplate") JdbcTemplate hdsJdbcTemplate,
            @Qualifier("cvpReportingJdbcTemplate") JdbcTemplate cvpReportingJdbcTemplate,
            ComponentHistoryService componentHistoryService,
            SSLSocketFactory sslSocketFactory,
            HostnameVerifier hostnameVerifier,
            MeterRegistry meterRegistry) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
        this.hdsJdbcTemplate = hdsJdbcTemplate;
        this.cvpReportingJdbcTemplate = cvpReportingJdbcTemplate;
        this.componentHistoryService = componentHistoryService;
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
        this.meterRegistry = meterRegistry;
    }

    public List<ComponentStatus> status() {
        return pcceProperties.getComponents().parallelStream()
                .map(this::probe)
                .toList();
    }

    private ComponentStatus probe(ComponentTarget target) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        ComponentStatus status;
        if (!target.isEnabled()) {
            status = new ComponentStatus(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                    ComponentState.DISABLED, target.getProbe(), describeTarget(target), 0,
                    "Probe disabled in configuration", checkedAt);
            componentHistoryService.record(status);
            updateStatusGauge(status);
            return status;
        }

        try {
            switch (target.getProbe()) {
                case HOST -> hostProbe(target);
                case TCP -> tcpProbe(target);
                case HTTP -> httpProbe(target);
                case JDBC_AW -> jdbcProbe(awJdbcTemplate, "SELECT 1");
                case JDBC_HDS -> jdbcProbe(hdsJdbcTemplate, "SELECT 1");
                case JDBC_CVP_REPORTING -> jdbcProbe(cvpReportingJdbcTemplate, "SELECT FIRST 1 1 FROM systables");
            }
            status = new ComponentStatus(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                    ComponentState.UP, target.getProbe(), describeTarget(target),
                    elapsedMs(start), "OK", checkedAt);
        } catch (Exception ex) {
            status = new ComponentStatus(target.getName(), displayName(target), target.getSide(), target.getSite(), target.getTier(),
                    ComponentState.DOWN, target.getProbe(), describeTarget(target),
                    elapsedMs(start), ex.getMessage(), checkedAt);
        }
        componentHistoryService.record(status);
        updateStatusGauge(status);
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
        if (connection instanceof HttpsURLConnection httpsConnection) {
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
            httpsConnection.setHostnameVerifier(hostnameVerifier);
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

    private void hostProbe(ComponentTarget target) throws IOException {
        if (target.getHost() == null || target.getHost().isBlank()) {
            throw new IllegalArgumentException("HOST probe requires host");
        }
        InetAddress.getByName(target.getHost());
    }

    private void jdbcProbe(JdbcTemplate jdbcTemplate, String sql) {
        jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private String describeTarget(ComponentTarget target) {
        return switch (target.getProbe()) {
            case HOST -> target.getHost();
            case HTTP -> target.getUrl();
            case TCP -> target.getHost() + ":" + target.getPort();
            case JDBC_AW -> "AW database";
            case JDBC_HDS -> "HDS database";
            case JDBC_CVP_REPORTING -> "CVP reporting database";
        };
    }

    private String displayName(ComponentTarget target) {
        return org.springframework.util.StringUtils.hasText(target.getDisplayName())
                ? target.getDisplayName()
                : target.getName().name();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void updateStatusGauge(ComponentStatus status) {
        String key = status.name() + "|" + nullSafe(status.side()) + "|" + nullSafe(status.site());
        AtomicReference<Double> value = componentStatusGauges.computeIfAbsent(key, ignored -> {
            AtomicReference<Double> reference = new AtomicReference<>(-1.0);
            Gauge.builder("pcce.component.status", reference, AtomicReference::get)
                    .description("PCCE component status: UP=1, DOWN=0, DISABLED=-0.5, UNKNOWN=-1")
                    .tag("name", String.valueOf(status.name()))
                    .tag("display_name", nullSafe(status.displayName()))
                    .tag("side", nullSafe(status.side()))
                    .tag("site", nullSafe(status.site()))
                    .tag("tier", nullSafe(status.tier()))
                    .register(meterRegistry);
            return reference;
        });
        value.set(statusValue(status.state()));
    }

    private double statusValue(ComponentState state) {
        if (state == null) {
            return -1.0;
        }
        return switch (state) {
            case UP -> 1.0;
            case DOWN -> 0.0;
            case DISABLED -> -0.5;
            case UNKNOWN -> -1.0;
        };
    }

    private String nullSafe(Object value) {
        return value == null ? "unknown" : value.toString();
    }
}
