package com.pig4cloud.pig.mcp.client.manager;

import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.client.manager.task.McpCommonTask;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.properties.CommandExecProperties;
import com.pig4cloud.pig.mcp.common.properties.CommandExecRequestProperties;
import com.pig4cloud.pig.mcp.common.util.HttpSseUrlUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManagerMcpSyncClientServiceImpl implements ManagerMcpSyncClientService {
    @Autowired
    private CommandExecProperties commandExecProperties;
    @Autowired
    private CommandExecRequestProperties commandExecRequestProperties;
    //同步mcp client 注册容器 <agentId,<clientName,McpClientInfo>>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> mcpSyncClientRegistryMap = new ConcurrentHashMap<>();
    @Resource(name = "mcpClientHeartbeatThreadPool")
    private ThreadPoolExecutor mcpClientHeartbeatThreadPool;

    @Resource(name = "mcpClientInitThreadPool")
    private ThreadPoolExecutor mcpClientInitThreadPool;

    /**
     * 注册并初始化 MCP 客户端。
     * <p>
     * 该方法负责将 MCP 客户端注册到注册表中，并进行初始化。
     * 具体步骤包括：
     * 1. 尝试获取指定 Agent ID 的客户端映射表。
     * 2. 如果映射表存在：
     * a. 检查是否已存在具有相同客户端名称的客户端。
     * b. 如果不存在，则初始化客户端并将其添加到映射表中。
     * c. 如果已存在，则检查现有客户端的健康状况。
     * d. 如果现有客户端不健康，则初始化新客户端，移除旧客户端，并将新客户端添加到映射表中。
     * 3. 如果映射表不存在：
     * a. 初始化客户端。
     * b. 创建新的客户端映射表。
     * c. 将客户端添加到映射表中。
     * d. 将映射表添加到主注册表中。
     *
     * @param agentId    Agent 的唯一标识符。
     * @param clientName 客户端的名称，用于在映射表中标识客户端。
     * @param mcpConfig  要注册和初始化的 MCP 客户端信息。
     */
    @Override
    public void registryAndInitMcpSyncClient(String agentId, String clientName, ChatRequestParams.McpConfig mcpConfig) {
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpSyncClientRegistryMap.get(agentId);
        if (mcpClientMapByAgentId != null) {
            McpClientInfo oldClientInfo = mcpClientMapByAgentId.get(clientName);
            if (oldClientInfo == null) {
                McpClientInfo mcpClientInfo = buildMcpClientInfo(mcpConfig);
                if (mcpClientInfo.initMcpSyncClient(agentId, clientName, mcpClientInfo, McpCommonTask.MCP_CLIENT_INIT_SECONDS)) {
                    mcpClientMapByAgentId.put(clientName, mcpClientInfo);
                } else {
                    mcpClientInfo.closeMcpSyncClient(agentId, clientName, mcpClientInfo.getMcpSyncClient());
                }
            } else if (!oldClientInfo.startHealthCheck(clientName, oldClientInfo, McpCommonTask.MCP_PING_TIMEOUT_SECONDS)) {
                log.warn("【MCP SYNC CLIENT MANAGER】 发现旧的client {} 健康检查失败, 进行替换", clientName); // 添加warn日志
                McpClientInfo mcpClientInfo = buildMcpClientInfo(mcpConfig);
                if (mcpClientInfo.initMcpSyncClient(agentId, clientName, mcpClientInfo, McpCommonTask.MCP_CLIENT_INIT_SECONDS)) {
                    mcpClientMapByAgentId.remove(clientName);
                    mcpClientMapByAgentId.put(clientName, mcpClientInfo);
                } else {
                    mcpClientInfo.closeMcpSyncClient(agentId, clientName, mcpClientInfo.getMcpSyncClient());
                }
            } else {
                oldClientInfo.access();
            }
        } else {
            McpClientInfo mcpClientInfo = buildMcpClientInfo(mcpConfig);
            if (mcpClientInfo.initMcpSyncClient(agentId, clientName, mcpClientInfo, McpCommonTask.MCP_CLIENT_INIT_SECONDS)) {
                mcpClientMapByAgentId = new ConcurrentHashMap<>();
                mcpClientMapByAgentId.put(clientName, mcpClientInfo);
                mcpSyncClientRegistryMap.put(agentId, mcpClientMapByAgentId);
            } else {
                mcpClientInfo.closeMcpSyncClient(agentId, clientName, mcpClientInfo.getMcpSyncClient());
            }
        }
    }


    /**
     * 根据 Agent ID 获取对应的 MCP 客户端映射表。
     * <p>
     * 该方法从 `mcpSyncClientRegistryMap` 中检索与给定 Agent ID 关联的
     * `ConcurrentHashMap<String, McpClientInfo>` 实例。  此映射表包含该 Agent 所注册的所有 MCP 客户端。
     *
     * @param agentId Agent 的唯一标识符，用于在注册表中查找相应的客户端映射表。
     * @return 与指定 Agent ID 关联的 `ConcurrentHashMap<String, McpClientInfo>` 实例。
     * 如果不存在与该 Agent ID 关联的映射表，则返回 `null`。
     */
    public ConcurrentHashMap<String, McpClientInfo> getMcpClientMapByAgentId(String agentId) {
        return mcpSyncClientRegistryMap.get(agentId);
    }


    /**
     * 获取所有 MCP 客户端的注册信息。
     * <p>
     * 该方法返回一个嵌套的并发哈希映射表，包含所有 Agent ID 及其对应的客户端信息。
     * 具体结构为：`ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>>`，
     * 其中：
     * - 外层 `ConcurrentHashMap` 的键为 Agent ID（字符串类型），值为内层 `ConcurrentHashMap`。
     * - 内层 `ConcurrentHashMap` 的键为客户端名称（字符串类型），值为具体的 `McpClientInfo` 实例。
     *
     * @return 包含所有 MCP 客户端注册信息的嵌套映射表。
     */
    @Override
    public ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> allMcpClientMap() {
        return this.mcpSyncClientRegistryMap;
    }


    /**
     * 根据客户端名称移除 MCP 客户端
     *
     * @param agentId    智能体 ID
     * @param clientName 客户端名称
     */
    @Override
    public void removeMcpClientByClientName(String agentId, String clientName) {
        // 根据智能体 ID 获取客户端 Map
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpSyncClientRegistryMap.get(agentId);
        // 判断客户端 Map 是否存在，并且客户端是否存在
        if (mcpClientMapByAgentId != null && mcpClientMapByAgentId.get(clientName) != null) {
            // 获取 MCP 同步客户端
            McpClientInfo mcpClientInfo = mcpClientMapByAgentId.get(clientName);
            // 关闭 MCP 同步客户端
            mcpClientInfo.closeMcpSyncClient(agentId, clientName, mcpClientInfo.getMcpSyncClient());
            // 移除客户端
            mcpClientMapByAgentId.remove(clientName);
        }
    }

    /**
     * 根据智能体 ID 移除 MCP 客户端
     *
     * @param agentId 智能体 ID
     */
    @Override
    public void removeMcpClientByAgentId(String agentId) {
        // 根据智能体 ID 获取客户端 Map
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpSyncClientRegistryMap.get(agentId);
        // 判断客户端 Map 是否存在，并且不为空
        if (mcpClientMapByAgentId != null && !mcpClientMapByAgentId.isEmpty()) {
            // 遍历客户端 Map，关闭所有客户端
            mcpClientMapByAgentId.forEach((clientName, mcpClientInfo) -> {
                mcpClientInfo.closeMcpSyncClient(agentId, clientName, mcpClientInfo.getMcpSyncClient());
            });
            // 清空客户端 Map
            mcpClientMapByAgentId.clear();
            // 移除智能体 ID
            mcpSyncClientRegistryMap.remove(agentId);
        }
    }

    /**
     * 初始化 MCP 同步客户端。
     * <p>
     * 该方法调用 `mcpClientInfo.getMcpSyncClient().initialize()` 来初始化客户端。
     * 如果初始化过程中发生异常，则会记录错误日志。
     *
     * @param agentId       Agent 的唯一标识符。
     * @param clientName    客户端的名称。
     * @param mcpClientInfo 包含要初始化的 MCP 客户端实例的 McpClientInfo 对象。
     */


    /**
     * 根据 McpConfig 构建 McpClientInfo 实例，其中包含 McpSyncClient 实例。
     * 如果 McpConfig 中包含 URL，则构建基于 HTTP SSE (Server-Sent Events) 的 McpSyncClient；
     * 否则，构建基于标准输入/输出 (stdio) 的 McpSyncClient。
     *
     * @param mcpConfig McpConfig 参数，包含 URL 等信息。
     * @return 构建好的 McpClientInfo 实例。
     */
    private McpClientInfo buildMcpClientInfo(ChatRequestParams.McpConfig mcpConfig) {
        // 如果 mcpConfig 包含有效的 url，则构建基于 Http Sse 的客户端
        return StringUtils.hasText(mcpConfig.url()) ? buildHttpSyncClientSseClient(mcpConfig) : buildStdioSyncClient(mcpConfig);
    }

    /**
     * 构建 同步 HTTP 客户端
     *
     * @param mcpConfig MCP 配置
     * @return 包含 MCP 同步客户端信息的 McpClientInfo 对象
     */
    public McpClientInfo buildHttpSyncClientSseClient(ChatRequestParams.McpConfig mcpConfig) {
        // 分割 URL
        Map<String, String> urlResult = HttpSseUrlUtils.splitUrl(mcpConfig.url());
        // 构建 HTTP 客户端传输对象
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(urlResult.get(HttpSseUrlUtils.DEFAULT_BASE_URL_NAME)).sseEndpoint(urlResult.get(HttpSseUrlUtils.DEFAULT_ENDPOINT_NAME)).build();
        // 构建 MCP 同步客户端
        McpSyncClient syncClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(commandExecRequestProperties.getTimeout())).build();
        return new McpClientInfo(McpClientType.SSE, mcpConfig.url(), syncClient, mcpConfig.defaultFlag(), new Date(), McpCommonTask.MCP_MAX_IDLE_MILLISECOND, mcpClientInitThreadPool, mcpClientHeartbeatThreadPool);
    }

    /**
     * 构建 同步 Stdio 客户端
     *
     * @param mcpConfig MCP 配置
     * @return 包含 MCP 同步客户端信息的 McpClientInfo 对象
     */
    private McpClientInfo buildStdioSyncClient(ChatRequestParams.McpConfig mcpConfig) {
        // 获取命令路径
        String commandPath = System.getProperty("os.name").toLowerCase().contains("win") ? commandExecProperties.getWinNode() : commandExecProperties.getNode();
        // 构建 ServerParameters 对象
        ServerParameters serverParameters = ServerParameters.builder(commandPath).args(mcpConfig.args()).env(mcpConfig.env()).build();
        // 构建 Stdio 客户端传输对象
        StdioClientTransport transport = new StdioClientTransport(serverParameters);
        // 构建 MCP 同步客户端
        McpSyncClient syncClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(commandExecRequestProperties.getTimeout())).build();
        return new McpClientInfo(McpClientType.STDIO, mcpConfig.name(), syncClient, mcpConfig.defaultFlag(), new Date(), McpCommonTask.MCP_MAX_IDLE_MILLISECOND, mcpClientInitThreadPool, mcpClientHeartbeatThreadPool);
    }
}
