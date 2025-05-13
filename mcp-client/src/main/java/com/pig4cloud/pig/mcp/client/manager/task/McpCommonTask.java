package com.pig4cloud.pig.mcp.client.manager.task;

import com.pig4cloud.pig.mcp.client.manager.ManagerMcpSyncClientService;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class McpCommonTask {
    public static Integer MCP_PING_TIMEOUT_SECONDS = 1; // Mcp Client Ping 超时时间，单位秒
    public static Integer MCP_CLIENT_INIT_SECONDS = 10; // Mcp Client init 超时时间,单位秒
    public static Long MCP_MAX_IDLE_MILLISECOND = 300000L; // Mcp Client 最大空闲时间时间,单位毫秒
    @Autowired
    private ManagerMcpSyncClientService managerMcpSyncClientService; // 注入 ManagerMcpSyncClientService，用于管理同步 MCP 客户端

    @Resource(name = "mcpClientHeartbeatThreadPool")
    private ThreadPoolExecutor mcpClientHeartbeatThreadPool; // 注入线程池，用于异步执行任务


    /**
     * MCP 客户端心跳检测任务
     * <p>
     * 使用 cron 表达式配置定时任务，默认每分钟执行一次
     */
    @Scheduled(cron = "${mcp.client.heart.beat.cron:0 */1 * * * ?}")
    public void mcpHeartbeatAndCheckIdle() {
        // 使用线程池异步执行心跳检测任务
        mcpClientHeartbeatThreadPool.execute(() -> {
            doMcpSyncClientHeartbeat(); // 执行同步客户端心跳检测
        });
    }


    /**
     * 执行同步 MCP 客户端心跳检测
     */
    private void doMcpSyncClientHeartbeat() {
        // 获取所有 MCP 客户端信息
        ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>> allMcpClientMap = managerMcpSyncClientService.allMcpClientMap();
        // 遍历所有客户端信息
        allMcpClientMap.forEach((agentId, clientInfoMap) -> {
            clientInfoMap.forEach((clientName, clientInfo) -> {
                // 为每个客户端提交一个检查任务
                mcpClientHeartbeatThreadPool.execute(() -> {
                    boolean healthy = clientInfo.startHealthCheck(clientName, clientInfo, MCP_PING_TIMEOUT_SECONDS);
                    boolean idle = clientInfo.checkIdle();
                    if (!healthy || idle) {
                        managerMcpSyncClientService.removeMcpClientByClientName(agentId, clientName);
                    }
                });
            });
        });
    }
}
