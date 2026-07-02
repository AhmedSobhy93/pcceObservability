package com.cisco.cx.observability.feature.notifications.web;

import com.cisco.cx.observability.config.PcceProperties.AlertSeverity;
import java.util.List;

public record UpdateNotificationSettingsRequest(
        Boolean webhookEnabled,
        Boolean smtpEnabled,
        String smtpFrom,
        List<String> smtpRecipients,
        String smtpSubjectPrefix,
        Boolean smsEnabled,
        String smsUrl,
        String smsAuthorization,
        String smsUserAgent,
        List<String> smsRecipients,
        AlertSeverity minimumSeverity,
        AlertSeverity smsMinimumSeverity,
        Integer smsMaxAlertsPerAssessment
) {
}
