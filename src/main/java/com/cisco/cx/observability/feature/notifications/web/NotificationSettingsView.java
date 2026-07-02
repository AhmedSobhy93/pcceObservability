package com.cisco.cx.observability.feature.notifications.web;

import com.cisco.cx.observability.config.PcceProperties.AlertSeverity;
import java.util.List;

public record NotificationSettingsView(
        boolean webhookEnabled,
        String webhookUrlConfigured,
        boolean smtpEnabled,
        String smtpFrom,
        List<String> smtpRecipients,
        String smtpSubjectPrefix,
        boolean smsEnabled,
        String smsUrlConfigured,
        String smsUserAgent,
        List<String> smsRecipients,
        AlertSeverity minimumSeverity,
        AlertSeverity smsMinimumSeverity,
        int smsMaxAlertsPerAssessment
) {
}
