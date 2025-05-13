package com.pig4cloud.pig.mcp.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Component
@EnableConfigurationProperties(CommandExecProperties.class)
@ConfigurationProperties(CommandExecProperties.CONFIG_PREFIX)
public class CommandExecProperties {
    public static final String CONFIG_PREFIX = "command.exec.path";

    private String winNode = "C:\\nvm4w\\nodejs\\npx.cmd";

    private String node = "/usr/local/bin/npx";

    private String winPython = "C:\\Program Files\\Python3X\\python.exe";

    private String python = "/usr/bin/python";

        private Integer requestTimeout = 30;
}
