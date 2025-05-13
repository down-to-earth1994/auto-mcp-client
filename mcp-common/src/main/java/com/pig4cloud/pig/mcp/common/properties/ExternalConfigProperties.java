package com.pig4cloud.pig.mcp.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@EnableConfigurationProperties(ExternalConfigProperties.class)
@ConfigurationProperties(ExternalConfigProperties.CONFIG_PREFIX)
public class ExternalConfigProperties {
    public static final String CONFIG_PREFIX = "mcp.config";
    private String jsonFilePath;

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }
}
