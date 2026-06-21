package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties;
import com.example.pcceobservability.model.AuditEvent;
import com.example.pcceobservability.security.AccessControlService;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    }
}
