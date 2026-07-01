package com.cisco.cx.observability.platform.config;

import com.cisco.cx.observability.service.QueryPerformanceService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {

    public MicrometerConfig(MeterRegistry meterRegistry, QueryPerformanceService queryPerformanceService) {
        Gauge.builder("cx.query.performance.tracked", queryPerformanceService, service -> service.recent(500).size())
                .description("Number of retained query performance events")
                .register(meterRegistry);
    }
}
