package com.cisco.cx.observability.feature.monitoring.domain;

import com.cisco.cx.observability.feature.projectplan.domain.ProjectTaskDto;
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
