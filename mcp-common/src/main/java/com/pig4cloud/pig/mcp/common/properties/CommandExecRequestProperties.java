package com.pig4cloud.pig.mcp.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@EnableConfigurationProperties(CommandExecRequestProperties.class)
@ConfigurationProperties(CommandExecRequestProperties.CONFIG_PREFIX)
public class CommandExecRequestProperties {
    public static final String CONFIG_PREFIX = "command.exec.request";
    //单位秒
    private Integer timeout = 30;


}
