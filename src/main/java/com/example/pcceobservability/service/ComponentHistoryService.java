package com.example.pcceobservability.service;

import com.example.pcceobservability.config.PcceProperties.ComponentName;
import com.example.pcceobservability.model.ComponentStatus;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ComponentHistoryService {

    private final Deque<ComponentStatus> history = new ArrayDeque<>();

    public synchronized void record(ComponentStatus status) {
        history.addFirst(status);
        while (history.size() > 2000) {
            history.removeLast();
        }
    }

    public synchronized List<ComponentStatus> recent(int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 500));
        return history.stream().limit(effectiveLimit).toList();
    }

    public synchronized List<ComponentStatus> recent(ComponentName name, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 500));
        return history.stream()
                .filter(status -> status.name() == name)
                .limit(effectiveLimit)
                .toList();
    }
}
