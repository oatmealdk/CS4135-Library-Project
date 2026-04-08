package com.elibrary.borrowing_service.infrastructure.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * @LoadBalanced tells Spring Cloud to intercept calls made with this
     * RestTemplate and resolve logical service names (e.g. "book-service")
     * to real instances via Eureka
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
