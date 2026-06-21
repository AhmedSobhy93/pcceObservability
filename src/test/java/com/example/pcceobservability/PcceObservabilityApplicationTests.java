package com.example.pcceobservability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "pcce.operations.scheduled-assessment-enabled=false"
})
class PcceObservabilityApplicationTests {

    @Test
    void contextLoads() {
    }
}
