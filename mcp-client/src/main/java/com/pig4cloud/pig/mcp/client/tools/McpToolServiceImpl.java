package com.pig4cloud.pig.mcp.client.tools;


import com.google.common.collect.Lists;
import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import com.pig4cloud.pig.mcp.client.manager.ManagerMcpSyncClientService;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.client.manager.task.McpCommonTask;
import com.pig4cloud.pig.mcp.client.registry.RegistryMcpClientService;
import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.model.McpToolInfo;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.util.R;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class McpToolServiceImpl implements McpToolService {
    @Autowired
    private ManagerMcpSyncClientService managerMcpSyncClientService;

    @Autowired
    private RegistryMcpClientService registryMcpClientService;

    public McpToolInfo listToolsResult(String agentId, ChatRequestParams.McpConfig mcpConfig) {
        String clientName = McpClientType.SSE.getName().equals(mcpConfig.type()) ? mcpConfig.url() : mcpConfig.name();
        managerMcpSyncClientService.registryAndInitMcpSyncClient(agentId, clientName, mcpConfig);
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = managerMcpSyncClientService.getMcpClientMapByAgentId(agentId);
        if (mcpClientMapByAgentId == null || mcpClientMapByAgentId.isEmpty()) {
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "获取工具描述信息失败"));
        }
        McpClientInfo clientInfo = mcpClientMapByAgentId.get(clientName);
        if (clientInfo == null) {
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "获取工具描述信息失败"));
        }
        McpSyncClient mcpSyncClient = clientInfo.getMcpSyncClient();
        McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();
        return new McpToolInfo(mcpConfig.name(), listToolsResult);
    }

    public List<McpToolInfo> multipleMcpClientToolsResult(String agentId, List<ChatRequestParams.McpConfig> mcpConfigs) {
        List<McpToolInfo> list = Lists.newArrayList();
        for (ChatRequestParams.McpConfig mcpConfig : mcpConfigs) {
            try {
                list.add(listToolsResult(agentId, mcpConfig));
            } catch (Exception e) {
                log.error("【MCP获取工具】 获取MCP SERVER TOOLS 失败,智能体id:{} MCP工具名称:{}", agentId, mcpConfig.name(), e);
            }
        }
        return list;
    }

    @Override
    public R checkMcpOnline(String agentId, String clientName) {
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = managerMcpSyncClientService.getMcpClientMapByAgentId(agentId);
        if(mcpClientMapByAgentId == null || mcpClientMapByAgentId.isEmpty()) {
            return R.failed();
        }
        McpClientInfo clientInfo = mcpClientMapByAgentId.get(clientName);
        if (clientInfo == null) {
            return R.failed();
        }
        boolean healthCheckFlag = clientInfo.startHealthCheck(clientName, clientInfo, McpCommonTask.MCP_PING_TIMEOUT_SECONDS);
        return healthCheckFlag ? R.ok() : R.failed();
    }
}
