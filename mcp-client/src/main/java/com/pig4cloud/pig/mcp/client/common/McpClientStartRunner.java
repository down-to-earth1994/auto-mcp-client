package com.pig4cloud.pig.mcp.client.common;

import com.pig4cloud.pig.mcp.client.registry.RegistryMcpClientService;
import com.pig4cloud.pig.mcp.client.resource.ResourceManagerService;
import com.pig4cloud.pig.mcp.common.constant.CommonConstants;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.properties.CommandExecProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class McpClientStartRunner implements ApplicationRunner {
    @Autowired
    private CommandExecProperties commandExecProperties;
    @Autowired
    private ResourceManagerService resourceManagerService;
    @Autowired
    private RegistryMcpClientService registryMcpClientService;


    @Override
    public void run(ApplicationArguments args) {
        checkNodePath();
//        registryGlobalMcpClient();
    }


    private void checkNodePath() {
        String nodePath;
        // 1. Determine the operating system
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        // 2. Select the correct Node.js executable path based on the OS
        if (isWindows) {
            nodePath = commandExecProperties.getWinNode();
            log.info("当前系统为 Windows，选择 Node.js 路径：{}", nodePath);
        } else {
            nodePath = commandExecProperties.getNode();
            log.info("当前系统为 Linux/macOS，选择 Node.js 路径：{}", nodePath);
        }
        // 3. Check if the Node.js executable file exists and is executable
        File nodeFile = new File(nodePath);
        if (!nodeFile.exists()) {
            log.error("Node.js 可执行文件不存在：{}", nodePath);
            // You might want to throw an exception or take other appropriate action here,
            // depending on your application's requirements.  For example:
            throw new IllegalStateException("Node.js executable not found at: " + nodePath);
        }
        if (!nodeFile.canExecute()) {
            log.error("Node.js 可执行文件不可执行：{}", nodePath);
            // Same as above, consider throwing an exception or taking other action.
            throw new IllegalStateException("Node.js executable is not executable at: " + nodePath);
        }
        log.info("Node.js 可执行文件检查通过，路径：{}", nodePath);
        // Optionally, you can set the validated path back to the properties object
        // commandExecProperties.setNode(nodePath); // If you want to ensure the property always holds the validated path.
    }


    private void registryGlobalMcpClient() {
        try {
            List<ChatRequestParams.McpConfig> mcpConfigs = resourceManagerService.loadJson(ChatRequestParams.McpConfig.class);
            registryMcpClientService.getToolCallbackProvider(true, CommonConstants.GLOBAL_ID, mcpConfigs);
        } catch (Exception e) {
            log.error("【系统初始化】初始化内置MCP_TOOLS失败", e);
        }

    }

}
