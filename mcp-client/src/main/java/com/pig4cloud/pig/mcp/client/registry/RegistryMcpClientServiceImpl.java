package com.pig4cloud.pig.mcp.client.registry;

import cn.hutool.core.collection.CollectionUtil;
import com.pig4cloud.pig.mcp.client.chat.CustomSyncMcpToolCallbackProvider;
import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import com.pig4cloud.pig.mcp.client.manager.ManagerMcpAsyncClientService;
import com.pig4cloud.pig.mcp.client.manager.ManagerMcpSyncClientService;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.util.R;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegistryMcpClientServiceImpl implements RegistryMcpClientService {

    @Autowired
    private ManagerMcpSyncClientService managerMcpSyncClientService; // 注入 ManagerMcpSyncClientService，用于管理同步 MCP 客户端
    @Autowired
    private ManagerMcpAsyncClientService managerMcpAsyncClientService; // 注入 ManagerMcpAsyncClientService，用于管理异步 MCP 客户端


    /**
     * 获取工具回调提供者
     *
     * @param stream     是否是流式请求
     * @param agentId    智能体 ID
     * @param mcpConfigs MCP 配置列表
     * @return 工具回调提供者数组
     * @throws RestCustomException MCP SERVER参数配置错误
     */
    @Override
    public ToolCallbackProvider[] getToolCallbackProvider(Boolean stream, String agentId, List<ChatRequestParams.McpConfig> mcpConfigs) {
        // 如果 MCP 配置列表为空，则返回空数组
        if (CollectionUtil.isEmpty(mcpConfigs)) {
            return new ToolCallbackProvider[0];
        }
        // 遍历 MCP 配置列表
        for (ChatRequestParams.McpConfig mcpConfig : mcpConfigs) {
            // 如果 MCP 服务器类型是 SSE
            if (McpClientType.SSE.getName().equals(mcpConfig.type())) {
                doRegisterHttpClientSseClient(stream, agentId, mcpConfig); // 注册 HTTP SSE 客户端
            }
            // 如果 MCP 服务器类型是 Stdio
            else if (McpClientType.STDIO.getName().equals(mcpConfig.type())) {
                doRegisterStdioClient(stream, agentId, mcpConfig); // 注册 Stdio 客户端
            }
            // 否则抛出异常
            else {
                throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "MCP SERVER参数配置错误"));
            }
        }
        // 获取指定智能体 ID 的 MCP 客户端 Map
        ConcurrentHashMap<String, McpClientInfo> mcpSyncClientMapByAgentId = managerMcpSyncClientService.getMcpClientMapByAgentId(agentId);
        ConcurrentHashMap<String, McpClientInfo> mcpAsyncClientMapByAgentId = managerMcpAsyncClientService.getMcpClientMapByAgentId(agentId);
        // 如果 MCP 同步客户端 Map 不为空
        if (mcpSyncClientMapByAgentId != null && !mcpSyncClientMapByAgentId.isEmpty()) {
            // 获取 MCP 同步客户端列表
            List<McpSyncClient> mcpSyncClientList = mcpSyncClientMapByAgentId.values().stream().map(McpClientInfo::getMcpSyncClient).collect(Collectors.toList());
            // 返回 CustomSyncMcpToolCallbackProvider 数组
            return new ToolCallbackProvider[]{new CustomSyncMcpToolCallbackProvider(mcpSyncClientList)};
        }
//        if (mcpSyncClientMapByAgentId != null && !mcpSyncClientMapByAgentId.isEmpty()) {
//            List<McpSyncClient> mcpSyncClientList = mcpSyncClientMapByAgentId.values().stream().map(McpClientInfo::getMcpSyncClient).collect(Collectors.toList());
//            return new ToolCallbackProvider[]{new CustomSyncMcpToolCallbackProvider(mcpSyncClientList)};
//        }
//        if (stream && mcpAsyncClientMapByAgentId != null && !mcpAsyncClientMapByAgentId.isEmpty()) {
//            List<McpAsyncClient> mcpAsyncClients = mcpAsyncClientMapByAgentId.values().stream().map(McpClientInfo::getMcpAsyncClient).collect(Collectors.toList());
//            return new ToolCallbackProvider[]{new CustomAsyncMcpToolCallbackProvider(mcpAsyncClients)};
//        }
        // 否则返回空数组
        else {
            return new ToolCallbackProvider[0];
        }
    }


    /**
     * 从提供的 ToolCallbackProvider 数组中提取所有有效的 FunctionCallback 对象，并将它们收集到一个列表中。
     * <p>
     * 该方法执行以下步骤：
     * 1. 将 ToolCallbackProvider 数组转换为 Stream。
     * 2. 过滤掉数组中的 null 元素。
     * 3. 对每个非 null 的 ToolCallbackProvider 调用 getToolCallbacks() 方法，获取 FunctionCallback 数组。
     * 4. 过滤掉获取到的 FunctionCallback 数组中的 null 数组。
     * 5. 将所有 FunctionCallback 数组合并为一个 Stream。
     * 6. 将 Stream 中的 FunctionCallback 对象收集到一个 List 中。
     *
     * @param stream     是否是流式请求
     * @param agentId    智能体 ID
     * @param mcpConfigs MCP 配置列表
     * @return 包含所有有效 FunctionCallback 对象的 List。
     */
    @Override
    public List<FunctionCallback> getFunctionCallbacks(Boolean stream, String agentId, List<ChatRequestParams.McpConfig> mcpConfigs) {
        // 获取 ToolCallbackProvider 数组
        ToolCallbackProvider[] mcpTools = this.getToolCallbackProvider(stream, agentId, mcpConfigs);
        // 将数组转换为 Stream，过滤掉 null 元素，获取 FunctionCallback 数组，过滤掉 null 数组，将 FunctionCallback 数组转换为 Stream，收集到 List 中
        return Arrays.stream(mcpTools) // 将数组转换为 Stream
                .filter(Objects::nonNull) // 过滤掉 null 元素
                .map(ToolCallbackProvider::getToolCallbacks) // 获取 FunctionCallback 数组
                .filter(Objects::nonNull) // 过滤掉 null 数组
                .flatMap(Arrays::stream) // 将 FunctionCallback 数组转换为 Stream
                .collect(Collectors.toList()); // 收集到 List 中
    }

    /**
     * 注册 HTTP 客户端
     *
     * @param stream     是否是流式请求
     * @param agentId    智能体 ID
     * @param mcpConfig MCP 配置
     */
    private void doRegisterHttpClientSseClient(Boolean stream, String agentId, ChatRequestParams.McpConfig mcpConfig) {
        // 注册并初始化 MCP 同步客户端
        managerMcpSyncClientService.registryAndInitMcpSyncClient(agentId, mcpConfig.url(), mcpConfig);
//        if (!stream) {
//            managerMcpSyncClientService.registryAndInitMcpSyncClient(agentId, mcpConfig.url(), mcpConfig);
//        } else {
//            managerMcpAsyncClientService.registryAndInitMcpAsyncClient(agentId, mcpConfig.name(), mcpConfig);
//        }

    }


    /**
     * 注册  Stdio 客户端
     *
     * @param stream     是否是流式请求
     * @param agentId   智能体 ID
     * @param mcpConfig MCP 配置
     */
    private void doRegisterStdioClient(Boolean stream, String agentId, ChatRequestParams.McpConfig mcpConfig) {
        // 查找 Stdio 客户端名称
        String stdioMcpServerName = mcpConfig.name();
        // 注册并初始化 MCP 同步客户端
        managerMcpSyncClientService.registryAndInitMcpSyncClient(agentId, stdioMcpServerName, mcpConfig);
//        // 注册并初始化客户端
//        if (!stream) {
//            managerMcpSyncClientService.registryAndInitMcpSyncClient(agentId, stdioMcpServerName, mcpConfig);
//        } else {
//            managerMcpAsyncClientService.registryAndInitMcpAsyncClient(agentId, stdioMcpServerName, mcpConfig);
//        }
    }
}
