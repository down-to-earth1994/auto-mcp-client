package com.pig4cloud.pig.mcp.client.chat;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class CustomSyncMcpToolCallbackProvider extends SyncMcpToolCallbackProvider {

    private final List<McpSyncClient> mcpClients;

    private final BiPredicate<McpSyncClient, McpSchema.Tool> toolFilter;

    /**
     * Constructor for CustomSyncMcpToolCallbackProvider with tool filter.
     *
     * @param toolFilter The filter to apply to tools.
     * @param mcpClients The list of MCP clients.
     */
    public CustomSyncMcpToolCallbackProvider(BiPredicate<McpSyncClient, McpSchema.Tool> toolFilter, List<McpSyncClient> mcpClients) {
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
    public CustomSyncMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        this((mcpClient, tool) -> true, mcpClients);
    }

    /**
     * Get the tool callbacks.
     *
     * @return An array of ToolCallback objects.
     */
    @Override
    public ToolCallback[] getToolCallbacks() {

        var toolCallbacks = new ArrayList<>();

        this.mcpClients.stream().forEach(mcpClient -> {
            toolCallbacks.addAll(mcpClient.listTools()
                    .tools()
                    .stream()
                    .filter(tool -> toolFilter.test(mcpClient, tool))
                    .map(tool -> new CustomSyncMcpToolCallback(mcpClient, tool))
                    .toList());
        });
        var array = toolCallbacks.toArray(new ToolCallback[0]);
        validateToolCallbacks(array);
        return array;
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
