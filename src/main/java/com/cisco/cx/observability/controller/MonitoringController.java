package com.cisco.cx.observability.controller;

import com.cisco.cx.observability.model.QueryPerformanceEvent;
import com.cisco.cx.observability.service.QueryPerformanceService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final QueryPerformanceService queryPerformanceService;
    private final Path logFile;

    public MonitoringController(
            QueryPerformanceService queryPerformanceService,
            @Value("${LOG_FILE:logs/cx-observability.log}") String logFile) {
        this.queryPerformanceService = queryPerformanceService;
        this.logFile = Path.of(logFile);
    }

    @GetMapping("/query-performance")
    @PreAuthorize("hasAuthority('PERM_OPERATIONS_READ')")
    public List<QueryPerformanceEvent> queryPerformance(@RequestParam(defaultValue = "100") int limit) {
        return queryPerformanceService.recent(limit);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('PERM_SOLUTION_ADMIN')")
    public List<String> logs(@RequestParam(defaultValue = "100") int limit) throws IOException {
        if (!Files.exists(logFile)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        int effectiveLimit = Math.max(1, Math.min(limit, 500));
        return lines.stream()
                .skip(Math.max(0, lines.size() - effectiveLimit))
                .toList();
    }
}
