package com.cisco.cx.observability.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.model.AuditEvent;
import com.cisco.cx.observability.security.AccessControlService;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final PcceProperties pcceProperties;
    private final AccessControlService accessControlService;
    private final Deque<AuditEvent> events = new ArrayDeque<>();

    public AuditService(PcceProperties pcceProperties, AccessControlService accessControlService) {
        this.pcceProperties = pcceProperties;
        this.accessControlService = accessControlService;
    }

    public synchronized void record(String action, String target, Map<String, Object> details) {
        String actor;
        try {
            actor = accessControlService.currentUser().username();
        } catch (Exception ex) {
            actor = "system";
        }
        AuditEvent event = new AuditEvent(Instant.now(), actor, action, target, details == null ? Map.of() : details);
        events.addFirst(event);
        log.info("audit actor={} action={} target={} details={}", actor, action, target, event.details());
        trim();
    }

    public synchronized List<AuditEvent> recent(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, pcceProperties.getOperations().getAuditRetention()));
        return events.stream().limit(effectiveLimit).toList();
    }

    private void trim() {
        int retention = Math.max(1, pcceProperties.getOperations().getAuditRetention());
        while (events.size() > retention) {
            events.removeLast();
        }
        purgeOlderThanRetentionDays();
    }

    @Scheduled(cron = "0 3 * * * *")
    public synchronized void purgeOldAuditEvents() {
        purgeOlderThanRetentionDays();
    }

    private void purgeOlderThanRetentionDays() {
        int retentionDays = Math.max(1, pcceProperties.getAudit().getRetentionDays());
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 24L * 60L * 60L);
        events.removeIf(event -> event.timestamp().isBefore(cutoff));
    }
}
