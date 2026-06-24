package com.example.pcceobservability.model;

public record TopicStat(
        int total,
        int completed,
        int inProgress,
        int onHold,
        int planned,
        int critical
) {
    public int pctComplete() {
        return total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
    }
}
