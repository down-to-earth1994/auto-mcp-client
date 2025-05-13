package com.pig4cloud.pig.mcp.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder builder() {
        return RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory());
    }
}