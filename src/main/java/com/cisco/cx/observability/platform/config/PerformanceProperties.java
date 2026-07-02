package com.cisco.cx.observability.platform.config;

import com.cisco.cx.observability.config.PcceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcce.performance")
public class PerformanceProperties extends PcceProperties.Performance {
}
