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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ComponentStatusService {

    private final PcceProperties pcceProperties;
    private final JdbcTemplate awJdbcTemplate;
    private final JdbcTemplate hdsJdbcTemplate;
    private final JdbcTemplate cvpReportingJdbcTemplate;

    public ComponentStatusService(
            PcceProperties pcceProperties,
            @Qualifier("awJdbcTemplate") JdbcTemplate awJdbcTemplate,
            @Qualifier("hdsJdbcTemplate") JdbcTemplate hdsJdbcTemplate,
            @Qualifier("cvpReportingJdbcTemplate") JdbcTemplate cvpReportingJdbcTemplate) {
        this.pcceProperties = pcceProperties;
        this.awJdbcTemplate = awJdbcTemplate;
        this.hdsJdbcTemplate = hdsJdbcTemplate;
        this.cvpReportingJdbcTemplate = cvpReportingJdbcTemplate;
    }

    public List<ComponentStatus> status() {
        return pcceProperties.getComponents().stream()
                .map(this::probe)
                .toList();
    }

    private ComponentStatus probe(ComponentTarget target) {
        Instant checkedAt = Instant.now();
        long start = System.nanoTime();
        if (!target.isEnabled()) {
            return new ComponentStatus(target.getName(), ComponentState.DISABLED, target.getProbe(), describeTarget(target), 0,
                    "Probe disabled in configuration", checkedAt);
        }

        try {
            switch (target.getProbe()) {
                case TCP -> tcpProbe(target);
                case HTTP -> httpProbe(target);
                case JDBC_AW -> jdbcProbe(awJdbcTemplate);
                case JDBC_HDS -> jdbcProbe(hdsJdbcTemplate);
                case JDBC_CVP_REPORTING -> jdbcProbe(cvpReportingJdbcTemplate);
            }
            return new ComponentStatus(target.getName(), ComponentState.UP, target.getProbe(), describeTarget(target),
                    elapsedMs(start), "OK", checkedAt);
        } catch (Exception ex) {
            return new ComponentStatus(target.getName(), ComponentState.DOWN, target.getProbe(), describeTarget(target),
                    elapsedMs(start), ex.getMessage(), checkedAt);
        }
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
        connection.setConnectTimeout((int) target.getTimeout().toMillis());
        connection.setReadTimeout((int) target.getTimeout().toMillis());
        connection.setRequestMethod("GET");
        int code = connection.getResponseCode();
        if (code < 200 || code >= 500) {
            throw new IOException("HTTP status " + code);
        }
    }

    private void jdbcProbe(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
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
