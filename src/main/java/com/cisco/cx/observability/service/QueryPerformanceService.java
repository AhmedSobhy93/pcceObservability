package com.cisco.cx.observability.service;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.model.QueryPerformanceEvent;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QueryPerformanceService {

    private final PcceProperties pcceProperties;
    private final Deque<QueryPerformanceEvent> events = new ArrayDeque<>();

    public QueryPerformanceService(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    public synchronized void record(String name, long elapsedMs, boolean success, String error) {
        events.addFirst(new QueryPerformanceEvent(Instant.now(), name, elapsedMs, success, error));
        trim();
    }

    public synchronized List<QueryPerformanceEvent> recent(int limit) {
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
