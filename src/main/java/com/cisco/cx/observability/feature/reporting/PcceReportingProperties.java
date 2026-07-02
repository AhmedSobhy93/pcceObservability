package com.cisco.cx.observability.feature.reporting;

import com.cisco.cx.observability.config.PcceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcce.queries")
public class PcceReportingProperties extends PcceProperties.Queries {
}
