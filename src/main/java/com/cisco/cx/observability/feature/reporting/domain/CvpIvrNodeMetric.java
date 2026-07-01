package com.cisco.cx.observability.feature.reporting.domain;

import java.time.LocalDateTime;

public record CvpIvrNodeMetric(
        String callId,
        LocalDateTime callStartTime,
        LocalDateTime callEndTime,
        String callerNumber,
        String appName,
        String duration,
        String flag,
        Integer callDispositionId,
        String callDispositionFlagDesc
) {
}
