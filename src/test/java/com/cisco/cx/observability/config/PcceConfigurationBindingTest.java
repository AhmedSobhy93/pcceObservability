package com.cisco.cx.observability.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cisco.cx.observability.feature.integration.cvpapi.CvpApiProperties;
import com.cisco.cx.observability.feature.integration.pcceapi.PcceApiProperties;
import com.cisco.cx.observability.feature.reporting.PcceReportingProperties;
import com.cisco.cx.observability.platform.config.PerformanceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PcceConfigurationBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesTestConfig.class)
            .withPropertyValues(
                    "pcce.queries.call-metrics=select 42",
                    "pcce.queries.dropped-calls-enabled=false",
                    "pcce.performance.slow-query-warning-ms=1234",
                    "pcce.pcce-api.enabled=true",
                    "pcce.pcce-api.base-url=https://pcce.example.local",
                    "pcce.cvp-api.enabled=true",
                    "pcce.cvp-api.base-url=https://cvp.example.local");

    @Test
    void bindsDomainPropertiesWithoutBreakingLegacyFacade() {
        contextRunner.run(context -> {
            PcceReportingProperties reportingProperties = context.getBean(PcceReportingProperties.class);
            PerformanceProperties performanceProperties = context.getBean(PerformanceProperties.class);
            PcceApiProperties pcceApiProperties = context.getBean(PcceApiProperties.class);
            CvpApiProperties cvpApiProperties = context.getBean(CvpApiProperties.class);
            PcceProperties legacyFacade = context.getBean(PcceProperties.class);

            assertThat(reportingProperties.getCallMetrics()).isEqualTo("select 42");
            assertThat(reportingProperties.isDroppedCallsEnabled()).isFalse();
            assertThat(performanceProperties.getSlowQueryWarningMs()).isEqualTo(1234);
            assertThat(pcceApiProperties.isEnabled()).isTrue();
            assertThat(pcceApiProperties.getBaseUrl()).isEqualTo("https://pcce.example.local");
            assertThat(cvpApiProperties.isEnabled()).isTrue();
            assertThat(cvpApiProperties.getBaseUrl()).isEqualTo("https://cvp.example.local");

            assertThat(legacyFacade.getQueries().getCallMetrics()).isEqualTo("select 42");
            assertThat(legacyFacade.getQueries().isDroppedCallsEnabled()).isFalse();
            assertThat(legacyFacade.getPerformance().getSlowQueryWarningMs()).isEqualTo(1234);
            assertThat(legacyFacade.getPcceApi().isEnabled()).isTrue();
            assertThat(legacyFacade.getPcceApi().getBaseUrl()).isEqualTo("https://pcce.example.local");
            assertThat(legacyFacade.getCvpApi().isEnabled()).isTrue();
            assertThat(legacyFacade.getCvpApi().getBaseUrl()).isEqualTo("https://cvp.example.local");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            PcceProperties.class,
            PcceReportingProperties.class,
            PerformanceProperties.class,
            PcceApiProperties.class,
            CvpApiProperties.class
    })
    static class PropertiesTestConfig {
    }
}
