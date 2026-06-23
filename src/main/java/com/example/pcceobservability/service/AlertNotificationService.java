package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.OperationalAlert;
import com.example.pcceobservability.model.ProductionAssessment;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);

    private final PcceProperties pcceProperties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public AlertNotificationService(PcceProperties pcceProperties, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.pcceProperties = pcceProperties;
        this.mailSenderProvider = mailSenderProvider;
    }

    public void notifyAssessment(ProductionAssessment assessment) {
        PcceProperties.Notifications notifications = pcceProperties.getNotifications();
        if (notifications == null) {
            return;
        }
        List<OperationalAlert> alerts = assessment.alerts().stream()
                .filter(alert -> alert.severity().ordinal() >= notifications.getMinimumSeverity().ordinal())
                .toList();
        if (alerts.isEmpty()) {
            return;
        }
        if (notifications.isWebhookEnabled() && StringUtils.hasText(notifications.getWebhookUrl())) {
            try {
                post(notifications, assessment, alerts);
            } catch (Exception ex) {
                log.warn("alert_webhook_failed error={}", ex.getMessage());
            }
        }
        if (notifications.isSmtpEnabled() && notifications.getSmtpRecipients() != null
                && !notifications.getSmtpRecipients().isEmpty()) {
            try {
                sendMail(notifications, assessment, alerts);
            } catch (Exception ex) {
                log.warn("alert_smtp_failed error={}", ex.getMessage());
            }
        }
        List<OperationalAlert> smsAlerts = assessment.alerts().stream()
                .filter(alert -> alert.severity().ordinal() >= notifications.getSmsMinimumSeverity().ordinal())
                .limit(Math.max(1, notifications.getSmsMaxAlertsPerAssessment()))
                .toList();
        if (notifications.isSmsEnabled()
                && StringUtils.hasText(notifications.getSmsUrl())
                && StringUtils.hasText(notifications.getSmsAuthorization())
                && notifications.getSmsRecipients() != null
                && !notifications.getSmsRecipients().isEmpty()
                && !smsAlerts.isEmpty()) {
            try {
                sendSms(notifications, smsAlerts);
            } catch (Exception ex) {
                log.warn("alert_sms_failed error={}", ex.getMessage());
            }
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

    private void sendMail(
            PcceProperties.Notifications notifications,
            ProductionAssessment assessment,
            List<OperationalAlert> alerts) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notifications.getSmtpFrom());
        message.setTo(notifications.getSmtpRecipients().toArray(String[]::new));
        message.setSubject(notifications.getSmtpSubjectPrefix() + " " + alerts.size() + " operational alert(s)");
        message.setText(mailBody(assessment, alerts));
        mailSender.send(message);
        log.info("alert_smtp_sent alerts={} recipients={}", alerts.size(), notifications.getSmtpRecipients().size());
    }

    private void sendSms(PcceProperties.Notifications notifications, List<OperationalAlert> alerts) throws Exception {
        int sent = 0;
        for (OperationalAlert alert : alerts) {
            for (String recipient : notifications.getSmsRecipients()) {
                if (!StringUtils.hasText(recipient)) {
                    continue;
                }
                postSms(notifications, recipient.trim(), alert);
                sent++;
            }
        }
        log.info("alert_sms_sent messages={} alerts={}", sent, alerts.size());
    }

    private void postSms(
            PcceProperties.Notifications notifications,
            String recipient,
            OperationalAlert alert) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        HttpURLConnection connection = (HttpURLConnection) new URL(notifications.getSmsUrl()).openConnection();
        connection.setConnectTimeout((int) notifications.getTimeout().toMillis());
        connection.setReadTimeout((int) notifications.getTimeout().toMillis());
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("X-Correlation-Id", correlationId);
        connection.setRequestProperty("User-Agent", notifications.getSmsUserAgent());
        connection.setRequestProperty("Authorization", notifications.getSmsAuthorization());
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(smsPayload(recipient, smsText(alert)).getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("SMS gateway returned HTTP " + status + " correlationId=" + correlationId);
        }
    }

    private String mailBody(ProductionAssessment assessment, List<OperationalAlert> alerts) {
        StringBuilder builder = new StringBuilder();
        builder.append("PCCE Observability assessment: ").append(assessment.assessedAt()).append("\n\n");
        for (OperationalAlert alert : alerts) {
            builder.append(alert.severity()).append(" - ").append(alert.category()).append("\n");
            builder.append("Component: ").append(alert.component()).append("\n");
            builder.append("Message: ").append(alert.message()).append("\n");
            builder.append("Recommended action: ").append(alert.recommendedAction()).append("\n\n");
        }
        return builder.toString();
    }

    private String smsText(OperationalAlert alert) {
        String component = StringUtils.hasText(alert.component()) ? alert.component() : "PCCE";
        String message = alert.message() == null ? "" : alert.message();
        String action = alert.recommendedAction() == null ? "" : alert.recommendedAction();
        String text = "PCCE " + alert.severity() + " [" + component + "] " + message + " | " + action;
        return text.length() <= 450 ? text : text.substring(0, 447) + "...";
    }

    private String smsPayload(String recipient, String text) {
        return "{\"mobileNumber\":\"" + escape(recipient) + "\","
                + "\"message\":\"" + escape(text) + "\"}";
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
