package com.cisco.cx.observability.feature.integration.cvpapi;

import com.cisco.cx.observability.config.PcceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pcce.cvp-api")
public class CvpApiProperties extends PcceProperties.CvpApi {
}
