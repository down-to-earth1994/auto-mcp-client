package com.pig4cloud.pig.mcp;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import lombok.extern.slf4j.Slf4j;


@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Slf4j
public class McpClientApplication {

    public static String appName;
    public static String customerId;
    public static String serviceName;
    public static String serviceInstanceId;

    public static void main(String[] args) {
        System.setProperty("java.io.tmpdir", System.getProperty("user.dir") + File.separator + "tmp");
        SpringApplication.run(McpClientApplication.class, args);
        StringBuilder appinfo = new StringBuilder(20);
        appinfo.append(serviceName).append(StringUtils.SPACE).append(serviceInstanceId);
        log.info(appinfo + " Started Successfully!");
    }

    @Value("${app.name}")
    private void setAppName(String appName) {
        McpClientApplication.appName = appName;
    }

    @Value("${customer.id}")
    private void setCustomerId(String customerId) {
        McpClientApplication.customerId = customerId;
    }

    @Value("${service.name}")
    private void setServiceName(String serviceName) {
        McpClientApplication.serviceName = serviceName;
    }

    @Value("${service.instance.id}")
    private void setServiceInstanceId(String serviceInstanceId) {
        McpClientApplication.serviceInstanceId = serviceInstanceId;
    }
}
