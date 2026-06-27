package com.cisco.cx.observability;

import com.cisco.cx.observability.config.PcceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PcceProperties.class)
public class CxObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxObservabilityApplication.class, args);
    }
}
