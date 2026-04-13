package com.elibrary.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
    }
)
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
