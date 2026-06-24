package com.example.pcceobservability.model;

import java.util.List;

public record ResourceStat(
        String name,
        int total,
        int completed,
        int inProgress,
        int onHold,
        List<ProjectTaskDto> activeTasks,
        List<ProjectTaskDto> criticalOpen
) {
}
