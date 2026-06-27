package com.cisco.cx.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "pcce.operations.scheduled-assessment-enabled=false"
})
class CxObservabilityApplicationTests {

    @Test
    void contextLoads() {
    }
}
