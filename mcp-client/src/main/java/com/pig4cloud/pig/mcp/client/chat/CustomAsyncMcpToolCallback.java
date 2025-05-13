package com.pig4cloud.pig.mcp.client.chat;

import cn.hutool.json.JSONUtil;
import com.pig4cloud.pig.mcp.common.util.JsonUtil;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomAsyncMcpToolCallback extends AsyncMcpToolCallback {
    private final McpAsyncClient asyncMcpClient;

    private final McpSchema.Tool tool;
    /**
     * Creates a new {@code SyncMcpToolCallback} instance.
     *
     * @param asyncMcpClient the MCP client to use for tool execution
     * @param tool      the MCP tool definition to adapt
     */
    public CustomAsyncMcpToolCallback(McpAsyncClient asyncMcpClient, McpSchema.Tool tool) {
        super(asyncMcpClient, tool);
        this.asyncMcpClient = asyncMcpClient;
        this.tool = tool;
    }

    public String call(String functionInput) {
        try {
            Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
            // Note that we use the original tool name here, not the adapted one from
            // getToolDefinition
            return this.asyncMcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments)).map(response -> {
                if (response.isError() != null && response.isError()) {
                    log.warn("tools exec response error: {}", response);
                    throw new ToolExecutionException(new DefaultToolDefinition(tool.name(),tool.description(), JSONUtil.toJsonStr(tool.inputSchema())),new IllegalStateException("Error calling tool: " + response.content()));
                }
                return ModelOptionsUtils.toJsonString(response.content());
            }).block();
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("tools exec response error  tool info {}", JsonUtil.encodeToString(tool), e);
            throw new ToolExecutionException(new DefaultToolDefinition(tool.name(), tool.description(), JSONUtil.toJsonStr(tool.inputSchema())), new IllegalStateException("Error calling tool exception: " + e.getMessage()));
        }
    }

}
