package com.cisco.cx.observability.feature.integration.pcceapi;

import com.cisco.cx.observability.config.PcceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcce.pcce-api")
public class PcceApiProperties extends PcceProperties.PcceApi {
}
