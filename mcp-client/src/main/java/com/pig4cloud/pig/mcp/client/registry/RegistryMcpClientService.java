package com.pig4cloud.pig.mcp.client.registry;

import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;

public interface RegistryMcpClientService {
    ToolCallbackProvider[] getToolCallbackProvider(Boolean stream,String agentId,List<ChatRequestParams.McpConfig> mcpConfigs);

    List<FunctionCallback> getFunctionCallbacks(Boolean stream,String agentId,List<ChatRequestParams.McpConfig> mcpConfigs);
}
