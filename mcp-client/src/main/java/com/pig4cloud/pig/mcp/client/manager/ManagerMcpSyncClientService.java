package com.pig4cloud.pig.mcp.client.manager;



import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;

import java.util.concurrent.ConcurrentHashMap;

public interface ManagerMcpSyncClientService {
    void registryAndInitMcpSyncClient(String agentId, String clientName, ChatRequestParams.McpConfig mcpConfig);

    ConcurrentHashMap<String, McpClientInfo> getMcpClientMapByAgentId(String agentId);

    ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> allMcpClientMap();

    McpClientInfo buildHttpSyncClientSseClient(ChatRequestParams.McpConfig mcpConfig);

    void removeMcpClientByClientName(String agentId, String clientName);

    void removeMcpClientByAgentId(String agentId);

}
