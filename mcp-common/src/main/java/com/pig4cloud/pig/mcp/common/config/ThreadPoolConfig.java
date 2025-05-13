package com.pig4cloud.pig.mcp.common.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ThreadPoolConfig {
    /**
     * 获取cpu核心数
     */
    private static final int cores = Runtime.getRuntime().availableProcessors();

    /**
     * io密集型 默认cpu核心数
     */
    private static final int ioCores = (cores * 2) + 1;

    @Value("${sse.consumer.core.thread.pool:30}")
    private Integer sseConsumerCorePool; // SSE 消费者核心线程池大小

    @Value("${mcp.client.heartbeat.thread.pool:10}")
    private Integer mcpClientHeartbeatThreadPool; // MCP 客户端心跳检测线程池大小

    @Value("${mcp.client.init.thread.pool:10}")
    private Integer mcpClientInitThreadPool; // MCP 客户端初始化线程池大小


    /**
     * sse stream consumer 消费线程池
     * 拒绝策略:负反馈机制
     *
     * @return ThreadPoolExecutor
     */
    @Bean(name = "sseStreamConsumerThreadPool")
    public ThreadPoolExecutor sseStreamConsumerThreadPool() {
        String poolNamePre = "sse-stream-consumer-thread-pool"; // 线程池名称前缀
        return new ThreadPoolExecutor(
                sseConsumerCorePool, sseConsumerCorePool, // 核心线程数和最大线程数都设置为 sseConsumerCorePool
                365L, TimeUnit.DAYS, new LinkedBlockingQueue<>(1024), // 线程存活时间365天，阻塞队列大小为10
                new ThreadFactoryBuilder().setNameFormat(poolNamePre + "-%d").setUncaughtExceptionHandler((t, e) -> log.error("大模型sse响应推送线程异常,线程名称:{}", t.getName(), e)).build(), // 线程工厂，设置线程名称和异常处理器
                new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略，由调用线程执行
    }

    /**
     * mcp client 心跳检测线程池
     *
     * @return ThreadPoolExecutor
     */
    @Bean(name = "mcpClientHeartbeatThreadPool")
    public ThreadPoolExecutor mcpClientHeartbeatThreadPool() {
        String poolNamePre = "mcp-client-heartbeat-thread-pool"; // 线程池名称前缀
        return new ThreadPoolExecutor(
                mcpClientHeartbeatThreadPool, mcpClientHeartbeatThreadPool, // 核心线程数和最大线程数都设置为 mcpClientHeartbeatThreadPool
                365L, TimeUnit.DAYS, new LinkedBlockingQueue<>(1024),  // 线程存活时间365天，阻塞队列大小为10
                new ThreadFactoryBuilder().setNameFormat(poolNamePre + "-%d").setUncaughtExceptionHandler((t, e) -> log.error("MCP CLIENT 心跳检测线程异常,线程名称:{}", t.getName(), e)).build(), // 线程工厂，设置线程名称和异常处理器
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * mcp client 初始化链接线程池
     *
     * @return ThreadPoolExecutor
     */
    @Bean(name = "mcpClientInitThreadPool")
    public ThreadPoolExecutor mcpClientInitThreadPool() {
        String poolNamePre = "mcp-client-init-thread-pool"; // 线程池名称前缀
        return new ThreadPoolExecutor(
                mcpClientInitThreadPool, mcpClientInitThreadPool, // 核心线程数和最大线程数都设置为 mcpClientHeartbeatThreadPool
                365L, TimeUnit.DAYS, new LinkedBlockingQueue<>(1024),  // 线程存活时间365天，阻塞队列大小为10
                new ThreadFactoryBuilder().setNameFormat(poolNamePre + "-%d").setUncaughtExceptionHandler((t, e) -> log.error("MCP CLIENT 心跳检测线程异常,线程名称:{}", t.getName(), e)).build(), // 线程工厂，设置线程名称和异常处理器
                new ThreadPoolExecutor.CallerRunsPolicy());
    }


}
