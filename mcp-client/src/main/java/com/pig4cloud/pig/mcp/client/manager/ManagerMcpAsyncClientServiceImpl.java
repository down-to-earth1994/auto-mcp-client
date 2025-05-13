package com.pig4cloud.pig.mcp.client.manager;

import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.client.manager.task.McpCommonTask;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.properties.CommandExecProperties;
import com.pig4cloud.pig.mcp.common.properties.CommandExecRequestProperties;
import com.pig4cloud.pig.mcp.common.util.HttpSseUrlUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
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
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManagerMcpAsyncClientServiceImpl implements ManagerMcpAsyncClientService {
    @Autowired
    private CommandExecProperties commandExecProperties; // 注入 CommandExecProperties，用于获取命令执行相关配置
    @Autowired
    private CommandExecRequestProperties commandExecRequestProperties; // 注入 CommandExecRequestProperties，用于获取命令执行请求相关配置
    //异步mcp client 注册容器 <agentId,<clientName,McpClientInfo>>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> mcpAsyncClientRegistryMap = new ConcurrentHashMap<>();
    @Resource(name = "mcpClientHeartbeatThreadPool")
    private ThreadPoolExecutor mcpClientHeartbeatThreadPool; // 注入线程池，用于执行 MCP 客户端心跳检测任务

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
    public void registryAndInitMcpAsyncClient(String agentId, String clientName, ChatRequestParams.McpConfig mcpConfig) {
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpAsyncClientRegistryMap.get(agentId);
        if (mcpClientMapByAgentId != null) {
            McpClientInfo oldClient = mcpClientMapByAgentId.get(clientName);
            if (oldClient == null) {
                McpClientInfo clientInfo = buildMcpAsyncClientClientStrategy(mcpConfig);
                initMcpAsyncClient(agentId, clientName, clientInfo);
                mcpClientMapByAgentId.put(clientName, clientInfo);
            } else if (!startHealthCheck(clientName, oldClient, McpCommonTask.MCP_PING_TIMEOUT_SECONDS)) {
                log.warn("【MCP ASYNC CLIENT MANAGER】 发现旧的client {} 健康检查失败, 进行替换", clientName); // 添加warn日志
                McpClientInfo clientInfo = buildMcpAsyncClientClientStrategy(mcpConfig);
                initMcpAsyncClient(agentId, clientName, clientInfo);
                mcpClientMapByAgentId.remove(clientName);
                mcpClientMapByAgentId.put(clientName, clientInfo);
            }
        } else {
            McpClientInfo clientInfo = buildMcpAsyncClientClientStrategy(mcpConfig);
            initMcpAsyncClient(agentId, clientName, clientInfo);
            mcpClientMapByAgentId = new ConcurrentHashMap<>();
            mcpClientMapByAgentId.put(clientName, clientInfo);
            mcpAsyncClientRegistryMap.put(agentId, mcpClientMapByAgentId);
        }
    }


    /**
     * 根据 Agent ID 获取对应的 MCP 客户端映射表。
     * <p>
     * 该方法从 `mcpAsyncClientRegistryMap` 中检索与给定 Agent ID 关联的
     * `ConcurrentHashMap<String, McpClientInfo>` 实例。  此映射表包含该 Agent 所注册的所有 MCP 客户端。
     *
     * @param agentId Agent 的唯一标识符，用于在注册表中查找相应的客户端映射表。
     * @return 与指定 Agent ID 关联的 `ConcurrentHashMap<String, McpClientInfo>` 实例。
     * 如果不存在与该 Agent ID 关联的映射表，则返回 `null`。
     */
    public ConcurrentHashMap<String, McpClientInfo> getMcpClientMapByAgentId(String agentId) {
        return mcpAsyncClientRegistryMap.get(agentId);
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
    public ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> allMcpClientMap() {
        return this.mcpAsyncClientRegistryMap;
    }

    /**
     * 启动指定 MCP 服务器的健康检查。
     * <p>
     * 该方法通过调用 `mcpClientInfo.getMcpAsyncClient().ping().block()` 来检查 MCP 服务器的健康状态。
     * 如果 ping 成功，则认为服务器健康；如果 ping 失败（抛出异常），则认为服务器不健康。
     *  使用线程池异步执行ping命令，并设置超时时间，防止ping命令阻塞
     * @param clientName    要检查的 MCP 服务器的名称。用于日志记录。
     * @param mcpClientInfo 包含用于与 MCP 服务器通信的 `McpAsyncClient` 实例的 McpClientInfo 对象。
     * @param timeoutSeconds 超时时间
     * @return `true` 如果服务器健康（ping 成功），`false` 如果服务器不健康（ping 失败）。
     */

    public boolean startHealthCheck(String clientName, McpClientInfo mcpClientInfo, Integer timeoutSeconds) {
        Future<Object> future = null;
        try {
            // 提交ping任务到线程池
            future = mcpClientHeartbeatThreadPool.submit(() -> {
                try {
                    // 执行ping命令
                    return mcpClientInfo.getMcpAsyncClient().ping();
                } catch (Exception e) {
                    log.error("【MCP ASYNC CLIENT MANAGER】 ping {} 发生异常", clientName, e);
                    throw new RuntimeException(e); // 重新抛出异常，让 future 可以捕获
                }
            });
            Object pingResult = future.get(timeoutSeconds, TimeUnit.SECONDS); // 设置超时时间
            log.debug("【MCP ASYNC CLIENT MANAGER】 服务名称: {} ping success result:{} 心跳检查健康", clientName, pingResult);
            return true;
        } catch (TimeoutException e) {
            log.warn("【MCP ASYNC CLIENT MANAGER】 服务名称: {} ping 超时", clientName);
            future.cancel(true); // 中断任务
            return false;
        } catch (InterruptedException e) {
            log.warn("【MCP ASYNC CLIENT MANAGER】 服务名称: {} ping 被中断", clientName);
            return false;
        } catch (ExecutionException e) {
            log.error("【MCP ASYNC CLIENT MANAGER】 服务名称: {} ping 发生执行异常", clientName, e); // 获取原始异常
            return false;
        }
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
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpAsyncClientRegistryMap.get(agentId);
        // 判断客户端 Map 是否存在，并且客户端是否存在
        if (mcpClientMapByAgentId != null && mcpClientMapByAgentId.get(clientName) != null) {
            // 获取 MCP 客户端信息
            McpClientInfo clientInfo = mcpClientMapByAgentId.get(clientName);
            // 关闭 MCP 客户端
            closeMcpSyncClient(agentId, clientName, clientInfo.getMcpAsyncClient());
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
        ConcurrentHashMap<String, McpClientInfo> mcpClientMapByAgentId = mcpAsyncClientRegistryMap.get(agentId);
        // 判断客户端 Map 是否存在，并且不为空
        if (mcpClientMapByAgentId != null && !mcpClientMapByAgentId.isEmpty()) {
            // 遍历客户端 Map，关闭所有客户端
            mcpClientMapByAgentId.forEach((clientName, clientInfo) -> {
                closeMcpSyncClient(agentId, clientName, clientInfo.getMcpAsyncClient());
            });
            // 清空客户端 Map
            mcpClientMapByAgentId.clear();
            // 移除智能体 ID
            mcpAsyncClientRegistryMap.remove(agentId);
        }
    }

    /**
     * 初始化 MCP 异步客户端。
     * <p>
     * 该方法调用 `mcpClientInfo.getMcpAsyncClient().initialize().block()` 来初始化客户端。
     * 如果初始化过程中发生异常，则会记录错误日志。
     *
     * @param agentId       Agent 的唯一标识符。
     * @param clientName    客户端的名称。
     * @param mcpClientInfo 包含要初始化的 MCP 客户端实例的 McpClientInfo 对象。
     */
    private void initMcpAsyncClient(String agentId, String clientName, McpClientInfo mcpClientInfo) {
        try {
            log.debug("【MCP ASYNC CLIENT MANAGER】 开始初始化客户端, 智能体id:{}, 客户端名称:{}", agentId, clientName); // 添加debug日志
            mcpClientInfo.getMcpAsyncClient().initialize().block();
            log.debug("【MCP ASYNC CLIENT MANAGER】 客户端初始化成功, 智能体id:{}, 客户端名称:{}", agentId, clientName); // 添加debug日志
        } catch (Exception e) {
            log.error("【MCP ASYNC CLIENT MANAGER】 客户端初始化连接出错 智能体id:{}, 客户端名称:{}", agentId, clientName, e);
        }
    }

    /**
     * 关闭 MCP 异步客户端
     *
     * @param agentId        智能体 ID
     * @param clientName     客户端名称
     * @param mcpAsyncClient MCP 异步客户端
     */
    private void closeMcpSyncClient(String agentId, String clientName, McpAsyncClient mcpAsyncClient) {
        try {
            // 关闭 MCP 异步客户端
            mcpAsyncClient.close();
        } catch (Exception e) {
            // 记录错误日志
            log.error("【MCP ASYNC CLIENT MANAGER】 客户端关闭连接出错 智能体id:{},客户端名称:{}", agentId, clientName, e);
        }
    }


    /**
     * 根据 McpConfig 构建 McpClientInfo 实例，其中包含 McpAsyncClient 实例。
     * 如果 McpConfig 中包含 URL，则构建基于 HTTP SSE (Server-Sent Events) 的 McpAsyncClient；
     * 否则，构建基于标准输入/输出 (stdio) 的 McpAsyncClient。
     *
     * @param mcpConfig McpConfig 参数，包含 URL 等信息。
     * @return 构建好的 McpClientInfo 实例。
     */
    public McpClientInfo buildMcpAsyncClientClientStrategy(ChatRequestParams.McpConfig mcpConfig) {
        // 如果 mcpConfig 包含有效的 url，则构建基于 Http Sse 的客户端
        return StringUtils.hasText(mcpConfig.url()) ? buildHttpAsyncClientSseClient(mcpConfig) : buildStdioAsyncClient(mcpConfig);
    }

    /**
     * 构建 异步 HTTP 客户端
     *
     * @param mcpConfig MCP 配置
     * @return 包含 MCP 异步客户端信息的 McpClientInfo 对象
     */
    private McpClientInfo buildHttpAsyncClientSseClient(ChatRequestParams.McpConfig mcpConfig) {
        // 分割 URL
        Map<String, String> urlResult = HttpSseUrlUtils.splitUrl(mcpConfig.url());
        // 构建 HTTP 客户端传输对象
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(urlResult.get(HttpSseUrlUtils.DEFAULT_BASE_URL_NAME)).sseEndpoint(urlResult.get(HttpSseUrlUtils.DEFAULT_ENDPOINT_NAME)).build();
        // 构建 MCP 异步客户端
        McpAsyncClient asyncClient = McpClient.async(transport).requestTimeout(Duration.ofSeconds(commandExecRequestProperties.getTimeout())).build();
        return new McpClientInfo(McpClientType.SSE, mcpConfig.url(), asyncClient, mcpConfig.defaultFlag(), new Date());
    }

    /**
     * 构建 异步 Stdio 客户端
     *
     * @param mcpConfig MCP 配置
     * @return 包含 MCP 异步客户端信息的 McpClientInfo 对象
     */
    private McpClientInfo buildStdioAsyncClient(ChatRequestParams.McpConfig mcpConfig) {
        // 获取命令路径
        String commandPath = System.getProperty("os.name").toLowerCase().contains("win") ? commandExecProperties.getWinNode() : commandExecProperties.getNode();
        // 构建 ServerParameters 对象
        ServerParameters serverParameters = ServerParameters.builder(commandPath).args(mcpConfig.args()).env(mcpConfig.env()).build();
        // 构建 Stdio 客户端传输对象
        StdioClientTransport transport = new StdioClientTransport(serverParameters);
        // 构建 MCP 异步客户端
        McpAsyncClient asyncClient = McpClient.async(transport).requestTimeout(Duration.ofSeconds(commandExecRequestProperties.getTimeout())).build();
        return new McpClientInfo(McpClientType.STDIO, mcpConfig.name(), asyncClient, mcpConfig.defaultFlag(), new Date());
    }


}
