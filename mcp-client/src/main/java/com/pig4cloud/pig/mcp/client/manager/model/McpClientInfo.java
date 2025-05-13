package com.pig4cloud.pig.mcp.client.manager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 存储和管理单个 MCP 客户端实例的信息。
 * 包含客户端对象、类型、名称、注册时间、状态、访问时间等信息，
 * 并提供初始化、健康检查、访问控制和关闭等操作。
 */
@Getter
@Setter
@Slf4j
public class McpClientInfo {
    /**
     * 尝试获取锁的超时时间（毫秒）。
     */
    private final long TRY_LOCK_TIMEOUT_MS = 200;

    /**
     * MCP 同步客户端实例。
     */
    private McpSyncClient mcpSyncClient;

    /**
     * MCP 异步客户端实例。
     */
    private McpAsyncClient mcpAsyncClient;

    /**
     * 客户端类型 (SYNC 或 ASYNC)。
     */
    private McpClientType clientType;

    /**
     * 客户端的唯一名称。
     */
    private String clientName;

    /**
     * 客户端注册的时间戳。
     */
    private Date registerTime;

    /**
     * 标记是否为内置或默认客户端。
     */
    private Boolean defaultFlag;

    /**
     * 客户端空闲超时时间（毫秒）。超过此时间未被访问的客户端可能被视为不活跃。
     */
    private Long idleTimeoutMillis;

    /**
     * 最后一次访问时间的原子引用，确保线程安全更新。
     */
    private final AtomicReference<Instant> lastAccessTime = new AtomicReference<>(Instant.now());

    /**
     * 客户端是否有效的原子布尔标记，确保线程安全的读写状态。
     */
    private final AtomicBoolean isValid = new AtomicBoolean(true);

    /**
     * 可重入锁，用于保护对客户端状态（如 isValid）的并发访问和修改，特别是在 access 和 close 操作中。
     */
    @JsonIgnore
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 用于执行 MCP 客户端初始化任务的线程池。
     */
    @JsonIgnore
    private ThreadPoolExecutor mcpClientInitThreadPool;

    /**
     * 用于执行 MCP 客户端心跳检查任务的线程池。
     */
    @JsonIgnore
    private ThreadPoolExecutor mcpClientHeartbeatThreadPool;

    /**
     * 同步客户端构造函数。
     *
     * @param clientType                   客户端类型 (SYNC)。
     * @param clientName                   客户端名称。
     * @param mcpSyncClient                同步客户端实例。
     * @param defaultFlag                  是否为默认客户端。
     * @param registerTime                 注册时间
     * @param idleTimeoutMillis            链接最大空闲时间
     * @param mcpClientInitThreadPool      初始化线程池。
     * @param mcpClientHeartbeatThreadPool 心跳检查线程池。
     */
    public McpClientInfo(McpClientType clientType, String clientName, McpSyncClient mcpSyncClient, Boolean defaultFlag, Date registerTime, Long idleTimeoutMillis, ThreadPoolExecutor mcpClientInitThreadPool, ThreadPoolExecutor mcpClientHeartbeatThreadPool) {
        this.clientType = clientType;
        this.clientName = clientName;
        this.mcpSyncClient = mcpSyncClient;
        this.registerTime = registerTime;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.defaultFlag = defaultFlag;
        this.mcpClientInitThreadPool = mcpClientInitThreadPool;
        this.mcpClientHeartbeatThreadPool = mcpClientHeartbeatThreadPool;
        // 初始化时记录访问时间
        this.lastAccessTime.set(Instant.now());
    }

    /**
     * 异步客户端构造函数。
     *
     * @param clientType     客户端类型 (ASYNC)。
     * @param clientName     客户端名称。
     * @param mcpAsyncClient 异步客户端实例。
     * @param defaultFlag    是否为默认客户端。
     * @param registerTime   注册时间。
     */
    public McpClientInfo(McpClientType clientType, String clientName, McpAsyncClient mcpAsyncClient, Boolean defaultFlag, Date registerTime) {
        this.clientType = clientType;
        this.clientName = clientName;
        this.mcpAsyncClient = mcpAsyncClient;
        this.registerTime = registerTime;
        this.defaultFlag = defaultFlag;
        // 异步客户端目前不使用初始化和心跳线程池
        // this.mcpClientInitThreadPool = mcpClientInitThreadPool;
        // this.mcpClientHeartbeatThreadPool = mcpClientHeartbeatThreadPool;
        // 初始化时记录访问时间
        this.lastAccessTime.set(Instant.now());
    }


    /**
     * 初始化 MCP 同步客户端，支持超时控制和异常处理。
     * <p>
     * 此方法将初始化操作封装为异步任务，并提交至 {@code mcpClientInitThreadPool} 线程池中执行。
     * 使用 {@link Future} 控制最大等待时间 {@code timeout}，防止阻塞调用线程。
     * 初始化成功后会更新最后访问时间。
     *
     * @param agentId       Agent 的唯一标识符 (用于日志记录)。
     * @param clientName    客户端的名称 (用于日志记录)。
     * @param mcpClientInfo 包含要初始化的 MCP 客户端实例的 McpClientInfo 对象 (当前对象)。
     * @param timeout       最大等待时间（单位秒），用于控制初始化任务的超时。
     * @return 如果初始化在超时时间内成功完成，则返回 {@code true}；否则返回 {@code false}。
     */
    public boolean initMcpSyncClient(String agentId, String clientName, McpClientInfo mcpClientInfo, long timeout) {
        log.debug("【MCP SYNC CLIENT MANAGER】 开始初始化客户端, 智能体id:{}, 客户端名称:{}", agentId, clientName);

        // 提交初始化任务到线程池
        Future<?> future = mcpClientInitThreadPool.submit(() -> {
            mcpClientInfo.getMcpSyncClient().initialize(); // 执行实际的初始化逻辑
            mcpClientInfo.access(); // 初始化成功后，更新最后一次访问时间
        });

        try {
            // 在指定的超时时间内等待初始化完成
            future.get(timeout, TimeUnit.SECONDS);
            log.debug("【MCP SYNC CLIENT MANAGER】 客户端初始化成功, 智能体id:{}, 客户端名称:{}", agentId, clientName);
            return true;
        } catch (TimeoutException e) {
            future.cancel(true); // 超时后尝试取消任务
            log.error("【MCP SYNC CLIENT MANAGER】 初始化超时, 智能体id:{}, 客户端名称:{}, 超时: {} 秒", agentId, clientName, timeout, e);
            return false;
        } catch (InterruptedException e) {
            future.cancel(true); // 中断后尝试取消任务
            Thread.currentThread().interrupt(); // 恢复线程的中断状态
            log.error("【MCP SYNC CLIENT MANAGER】 初始化被中断, 智能体id:{}, 客户端名称:{}", agentId, clientName, e);
            return false;
        } catch (ExecutionException e) {
            // 任务执行过程中抛出异常
            log.error("【MCP SYNC CLIENT MANAGER】 初始化执行异常, 智能体id:{}, 客户端名称:{}", agentId, clientName, e.getCause() != null ? e.getCause() : e);
            return false;
        }
    }

    /**
     * 对指定的 MCP 同步客户端执行健康检查 (ping)。
     * <p>
     * 该方法将 ping 操作提交到 {@code mcpClientHeartbeatThreadPool} 线程池异步执行，
     * 并使用 {@link Future} 在指定的超时时间内等待结果。
     * 通过调用 {@code mcpClientInfo.getMcpSyncClient().ping()} 来检查 MCP 服务器的健康状态。
     * 如果 ping 成功，则认为服务器健康；如果 ping 失败（抛出异常或超时），则认为服务器不健康。
     *
     * @param clientName     要检查的 MCP 服务器的名称 (用于日志记录)。
     * @param mcpClientInfo  包含用于与 MCP 服务器通信的 {@code McpSyncClient} 实例的 McpClientInfo 对象 (当前对象)。
     * @param timeoutSeconds 最大等待时间（单位秒），用于控制 ping 操作的超时。
     * @return {@code true} 如果服务器健康（ping 在超时时间内成功），{@code false} 如果服务器不健康（ping 失败或超时）。
     */
    public boolean startHealthCheck(String clientName, McpClientInfo mcpClientInfo, long timeoutSeconds) {
        Future<Object> future = null;
        try {
            future = mcpClientHeartbeatThreadPool.submit(() -> {
                try {
                    // 执行实际的 ping 操作
                    return mcpClientInfo.getMcpSyncClient().ping();
                } catch (Exception e) {
                    // 在任务内部捕获异常，以便 future.get() 可以区分执行异常和超时/中断
                    log.error("【MCP SYNC CLIENT MANAGER】 ping {} 发生异常", clientName, e);
                    // 包装并重新抛出，以便 ExecutionException 能捕获它
                    throw new RuntimeException("Ping failed for " + clientName, e);
                }
            });
            // 在指定的超时时间内等待 ping 操作完成
            Object pingResult = future.get(timeoutSeconds, TimeUnit.SECONDS);
            log.debug("【MCP SYNC CLIENT MANAGER】 服务名称: {} ping success result:{} 心跳检查健康", clientName, pingResult);
            return true;
        } catch (TimeoutException e) {
            log.warn("【MCP SYNC CLIENT MANAGER】 服务名称: {} ping 超时 ({} 秒)", clientName, timeoutSeconds);
            if (future != null) {
                future.cancel(true); // 超时后尝试中断任务
            }
            return false;
        } catch (InterruptedException e) {
            log.warn("【MCP SYNC CLIENT MANAGER】 服务名称: {} ping 被中断", clientName);
            if (future != null) {
                future.cancel(true); // 中断后尝试取消任务
            }
            Thread.currentThread().interrupt(); // 恢复线程的中断状态
            return false;
        } catch (ExecutionException e) {
            // ping 任务执行过程中抛出异常 (由内部 RuntimeException 包装)
            log.error("【MCP SYNC CLIENT MANAGER】 服务名称: {} ping 发生执行异常", clientName, e.getCause() != null ? e.getCause() : e);
            return false;
        }
    }

    /**
     * 记录一次客户端访问，更新最后访问时间。
     * <p>
     * 此方法使用带超时的尝试锁 ({@code tryLock}) 来获取 {@link #lock}，以避免死锁和无限等待。
     * 在获取锁后，检查客户端是否仍然有效 ({@link #isValid})。
     * 如果有效，则更新 {@link #lastAccessTime} 为当前时间。
     *
     * @return 如果成功获取锁并在客户端有效时更新了访问时间，则返回 {@code true}。
     * 如果在超时时间内未能获取锁，或获取锁后发现客户端已失效，则返回 {@code false}。
     * 如果线程在尝试获取锁时被中断，也返回 {@code false}。
     */
    public boolean access() {
        try {
            // 尝试在指定超时时间内获取锁
            if (lock.tryLock(TRY_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                try {
                    // 双重检查：再次确认客户端是否有效
                    if (!isValid.get()) {
                        log.warn("【MCP Client INFO】客户端 {} 无效，拒绝访问", clientName);
                        return false; // 客户端已关闭或标记为无效
                    }
                    // 更新最后访问时间
                    Instant now = Instant.now();
                    now.atZone(ZoneId.systemDefault());
                    lastAccessTime.set(now);
                    // log.trace("【MCP Client INFO】客户端 {} 访问成功，更新访问时间", clientName); // 调试日志，可选
                    return true;
                } finally {
                    lock.unlock(); // 确保释放锁
                }
            } else {
                // 获取锁超时
                log.warn("【MCP Client INFO】客户端 {} 尝试访问时获取锁超时（{}ms），拒绝访问", clientName, TRY_LOCK_TIMEOUT_MS);
                return false;
            }
        } catch (InterruptedException e) {
            // 当前线程在等待锁时被中断
            log.warn("【MCP Client INFO】客户端 {} 尝试访问时获取锁被中断", clientName, e);
            Thread.currentThread().interrupt(); // 重新设置中断标志
            return false;
        }
    }

    /**
     * 检查客户端是否因空闲时间过长而超时。
     * <p>
     * 计算当前时间与 {@link #lastAccessTime} 之间的差值，
     * 并与配置的 {@link #idleTimeoutMillis} 进行比较。
     *
     * @return 如果当前空闲时间超过了 {@code idleTimeoutMillis}，则返回 {@code true}，表示空闲超时；否则返回 {@code false}。
     */
    public boolean checkIdle() {
        // 读取原子引用中的最后访问时间
        Instant lastAccess = lastAccessTime.get();
        Instant now = Instant.now();
        // 计算空闲时间（毫秒）
        long idleTimeMillis = now.toEpochMilli() - lastAccess.toEpochMilli();

        // 检查是否超过配置的空闲超时阈值
        if (idleTimeMillis > idleTimeoutMillis) {
            log.warn("【MCP Client】客户端 {} 空闲超时 {} ms，超过阈值 {} ms，可能需要关闭", clientName, idleTimeMillis, idleTimeoutMillis);
            return true;
        } else {
            // log.trace("【MCP Client】客户端 {} 空闲时间 {} ms，未超时 (阈值 {} ms)", clientName, idleTimeMillis, idleTimeoutMillis); // 调试日志，可选
            return false;
        }
    }

    /**
     * 关闭 MCP 同步客户端连接并标记为无效。
     * <p>
     * 此方法使用带超时的尝试锁 ({@code tryLock}) 来获取 {@link #lock}。
     * 获取锁后，使用 {@link AtomicBoolean#compareAndSet(boolean, boolean)} 确保 {@code close()} 操作只执行一次，
     * 并将客户端标记为无效 ({@code isValid = false})。
     * 实际的关闭操作委托给 {@link McpSyncClient#close()}。
     *
     * @param agentId    智能体 ID (用于日志记录)。
     * @param clientName 客户端名称 (用于日志记录)。
     * @param syncClient 要关闭的 MCP 同步客户端实例 (理论上可以从 this.mcpSyncClient 获取，传入参数可能是历史原因或特定场景需要)。
     *                   **注意:** 确保传入的 syncClient 与 this.mcpSyncClient 是同一个实例，否则可能关闭错误的对象。
     */
    public void closeMcpSyncClient(String agentId, String clientName, McpSyncClient syncClient) {
        try {
            // 尝试在指定超时时间内获取锁
            if (lock.tryLock(TRY_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                try {
                    // 使用 CAS 操作确保原子性地将 isValid 从 true 更新为 false
                    // 只有在状态从未关闭 (true) 变为关闭 (false) 时，才执行内部逻辑
                    if (isValid.compareAndSet(true, false)) {
                        log.info("【MCP Client INFO】客户端 {} 正在关闭连接...", clientName);
                        try {
                            if (this.mcpSyncClient != null) {
                                this.mcpSyncClient.close(); // 调用底层客户端的关闭方法
                                log.info("【MCP Client INFO】客户端 {} 连接已成功关闭。", clientName);
                            } else {
                                log.warn("【MCP Client INFO】尝试关闭客户端 {}，但 mcpSyncClient 实例为空。", clientName);
                            }
                        } catch (Exception e) {
                            // 底层 close() 方法可能抛出异常
                            log.error("【MCP SYNC CLIENT MANAGER】关闭客户端 {} 连接时发生异常。Agent ID: {}", clientName, agentId, e);
                        }
                    } else {
                        // CAS 操作失败，意味着 isValid 已经是 false，说明客户端已被关闭
                        log.warn("【MCP Client INFO】客户端 {} 已经被关闭，无需重复操作。", clientName);
                    }
                } finally {
                    lock.unlock(); // 确保释放锁
                }
            } else {
                // 获取锁超时
                log.warn("【MCP Client INFO】尝试关闭客户端 {} 时获取锁超时（{}ms），无法关闭。", clientName, TRY_LOCK_TIMEOUT_MS);
                // 考虑是否需要后续重试或其他处理机制
            }
        } catch (InterruptedException e) {
            // 当前线程在等待锁时被中断
            log.error("【MCP SYNC CLIENT MANAGER】尝试关闭客户端 {} 时获取锁被中断。Agent ID: {}", clientName, agentId, e);
            Thread.currentThread().interrupt(); // 重新设置中断标志
        }
    }

}
