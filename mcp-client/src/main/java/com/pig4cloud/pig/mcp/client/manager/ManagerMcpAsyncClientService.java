package com.pig4cloud.pig.mcp.client.manager;

import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;

import java.util.concurrent.ConcurrentHashMap;

public interface ManagerMcpAsyncClientService {
    void registryAndInitMcpAsyncClient(String agentId, String clientName, ChatRequestParams.McpConfig mcpConfig);

    ConcurrentHashMap<String, McpClientInfo> getMcpClientMapByAgentId(String agentId);

    boolean startHealthCheck(String clientName, McpClientInfo mcpClientInfo,Integer timeoutSeconds);

    ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> allMcpClientMap();

    void removeMcpClientByClientName(String agentId, String clientName);

    void removeMcpClientByAgentId(String agentId);


}
