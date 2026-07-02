package com.cisco.cx.observability;

import com.cisco.cx.observability.config.PcceProperties;
import com.cisco.cx.observability.feature.integration.cvpapi.CvpApiProperties;
import com.cisco.cx.observability.feature.integration.pcceapi.PcceApiProperties;
import com.cisco.cx.observability.feature.reporting.PcceReportingProperties;
import com.cisco.cx.observability.platform.config.PerformanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        PcceProperties.class,
        PcceReportingProperties.class,
        PerformanceProperties.class,
        PcceApiProperties.class,
        CvpApiProperties.class
})
public class CxObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxObservabilityApplication.class, args);
    }
}
