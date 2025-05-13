package com.pig4cloud.pig.mcp.client.tools;




import com.pig4cloud.pig.mcp.common.model.McpToolInfo;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.util.R;

import java.util.List;

public interface McpToolService {
    /**
     * 返回mcp tools info
     * @param agentId 智能体id
     * @param mcpConfig 单个mcp工具配置
     * @return McpToolInfo
     */
    McpToolInfo listToolsResult(String agentId, ChatRequestParams.McpConfig mcpConfig);

    /**
     * 返回多个mcp tools info
     * @param agentId 智能体id
     * @param mcpConfigs 多个mcp工具配置
     * @return McpToolInfo
     */
    List<McpToolInfo> multipleMcpClientToolsResult(String agentId, List<ChatRequestParams.McpConfig> mcpConfigs);

    /**
     *
     * @param agentId 智能体id
     * @param clientName 客户端名称
     * @return R
     */
    R checkMcpOnline(String agentId, String clientName);
}
