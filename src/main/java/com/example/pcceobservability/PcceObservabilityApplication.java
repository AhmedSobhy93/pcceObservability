package com.example.pcceobservability;

import com.example.pcceobservability.config.PcceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(PcceProperties.class)
public class PcceObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(PcceObservabilityApplication.class, args);
    }
}
