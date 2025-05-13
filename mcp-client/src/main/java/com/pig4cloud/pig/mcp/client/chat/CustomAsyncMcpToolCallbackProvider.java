package com.pig4cloud.pig.mcp.client.chat;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class CustomAsyncMcpToolCallbackProvider extends AsyncMcpToolCallbackProvider {

    private final List<McpAsyncClient> mcpClients;

    private final BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter;

    /**
     * Constructor for CustomSyncMcpToolCallbackProvider with tool filter.
     *
     * @param toolFilter The filter to apply to tools.
     * @param mcpClients The list of MCP clients.
     */
    public CustomAsyncMcpToolCallbackProvider(BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter, List<McpAsyncClient> mcpClients) {
        Assert.notNull(mcpClients, "MCP clients must not be null");
        Assert.notNull(toolFilter, "Tool filter must not be null");
        this.mcpClients = mcpClients;
        this.toolFilter = toolFilter;
    }

    /**
     * Constructor for CustomSyncMcpToolCallbackProvider without tool filter.
     *
     * @param mcpClients The list of MCP clients.
     */
    public CustomAsyncMcpToolCallbackProvider(List<McpAsyncClient> mcpClients) {
        this((mcpClient, tool) -> true, mcpClients);
    }

    /**
     * Get the tool callbacks.
     *
     * @return An array of ToolCallback objects.
     */
    @Override
    public ToolCallback[] getToolCallbacks() {

        List<ToolCallback> toolCallbackList = new ArrayList<>();

        for (McpAsyncClient mcpClient : this.mcpClients) {

            ToolCallback[] toolCallbacks = mcpClient.listTools()
                    .map(response -> response.tools()
                            .stream()
                            .filter(tool -> toolFilter.test(mcpClient, tool))
                            .map(tool -> new CustomAsyncMcpToolCallback(mcpClient, tool))
                            .toArray(ToolCallback[]::new))
                    .block();

            validateToolCallbacks(toolCallbacks);

            toolCallbackList.addAll(List.of(toolCallbacks));
        }

        return toolCallbackList.toArray(new ToolCallback[0]);
    }

    /**
     * Validate the tool callbacks to ensure there are no duplicate tool names.
     *
     * @param toolCallbacks An array of ToolCallback objects.
     * @throws IllegalStateException if there are duplicate tool names.
     */
    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException(
                    "Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
        }
    }
}
