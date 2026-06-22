package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.OperationalAlert;
import com.example.pcceobservability.model.ProductionAssessment;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    private final PcceProperties pcceProperties;

    public AlertNotificationService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    public void notifyAssessment(ProductionAssessment assessment) {
        PcceProperties.Notifications notifications = pcceProperties.getNotifications();
        if (notifications == null
                || !notifications.isWebhookEnabled()
                || !StringUtils.hasText(notifications.getWebhookUrl())) {
            return;
        }
        List<OperationalAlert> alerts = assessment.alerts().stream()
                .filter(alert -> alert.severity().ordinal() >= notifications.getMinimumSeverity().ordinal())
                .toList();
        if (alerts.isEmpty()) {
            return;
        }
        try {
            post(notifications, assessment, alerts);
        } catch (Exception ex) {
            log.warn("alert_webhook_failed error={}", ex.getMessage());
        }
    }

    private void post(
            PcceProperties.Notifications notifications,
            ProductionAssessment assessment,
            List<OperationalAlert> alerts) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(notifications.getWebhookUrl()).openConnection();
        connection.setConnectTimeout((int) notifications.getTimeout().toMillis());
        connection.setReadTimeout((int) notifications.getTimeout().toMillis());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        String body = payload(assessment, alerts);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("webhook returned HTTP " + status);
        }
        log.info("alert_webhook_sent alerts={}", alerts.size());
    }

    private String payload(ProductionAssessment assessment, List<OperationalAlert> alerts) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"source\":\"pcce-observability\",");
        builder.append("\"assessed_at\":\"").append(assessment.assessedAt()).append("\",");
        builder.append("\"alert_count\":").append(alerts.size()).append(",");
        builder.append("\"alerts\":[");
        for (int i = 0; i < alerts.size(); i++) {
            OperationalAlert alert = alerts.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{\"severity\":\"").append(alert.severity()).append("\",");
            builder.append("\"category\":\"").append(escape(alert.category())).append("\",");
            builder.append("\"component\":\"").append(escape(alert.component())).append("\",");
            builder.append("\"message\":\"").append(escape(alert.message())).append("\",");
            builder.append("\"recommended_action\":\"").append(escape(alert.recommendedAction())).append("\"}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
