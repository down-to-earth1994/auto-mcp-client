package com.pig4cloud.pig.mcp.client.chat;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.pig4cloud.pig.mcp.client.handle.StreamChatEventHandler;
import com.pig4cloud.pig.mcp.client.registry.RegistryMcpClientService;
import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.util.JsonUtil;
import com.pig4cloud.pig.mcp.common.util.R;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatServiceImpl 类，实现了 ChatService 接口，用于处理聊天相关的业务逻辑
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatModel chatModel; // 注入 ChatModel，用于与大模型进行交互

    @Autowired
    private ToolCallingManager toolCallingManager; // 注入 ToolCallingManager，用于处理工具调用

    @Resource(name = "sseStreamConsumerThreadPool")
    private ThreadPoolExecutor sseStreamConsumerThreadPool; // 注入线程池，用于异步执行任务

    @Autowired
    private StreamChatEventHandler streamChatEventHandler; // 注入 StreamChatEventHandler，用于处理流式聊天事件

    @Autowired
    private RegistryMcpClientService registryMcpSyncClientService; // 注入 RegistryMcpClientService，用于注册 MCP 同步客户端

    /**
     * 聊天接口
     *
     * @param chatRequestParams 聊天请求参数
     * @param response          HttpServletResponse
     * @return 聊天结果
     * @throws RestCustomException 对话消息不存在
     */
    @Override
    public Object chat(ChatRequestParams chatRequestParams, HttpServletResponse response) {
        // 将请求参数中的消息列表转换为 Message 对象列表
        List<Message> messageList = chatRequestParams.transMessage();
        // 如果消息列表为空，则抛出异常
        if (CollectionUtil.isEmpty(messageList)) {
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "对话消息不存在"));
        }
        // 执行聊天操作
        return doChat(chatRequestParams, response, messageList);
    }

    /**
     * 执行聊天操作
     *
     * @param chatRequestParams 聊天请求参数
     * @param response          HttpServletResponse
     * @param messageList       消息列表
     * @return 聊天结果
     */
    private Object doChat(ChatRequestParams chatRequestParams, HttpServletResponse response, List<Message> messageList) {
        // 根据是否是流式请求，选择不同的聊天方式
        return chatRequestParams.getStream() ? streamChat(chatRequestParams, messageList, response) : R.ok(normalChat(chatRequestParams, messageList));
    }

    /**
     * 正常普通询问
     *
     * @param chatRequestParams 聊天请求参数
     * @param messageList       问答列表
     * @return 大模型返回内容
     */
    private String normalChat(ChatRequestParams chatRequestParams, List<Message> messageList) {
        List<FunctionCallback> tools = Lists.newArrayList();
        try {
            // 获取 FunctionCallback 列表
            tools = registryMcpSyncClientService.getFunctionCallbacks(chatRequestParams.getStream(), chatRequestParams.getAgentId(), chatRequestParams.getMcpConfigs());
            ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder().toolCallbacks(tools).internalToolExecutionEnabled(chatRequestParams.getInternalToolExecutionEnabled()).build();
            // 创建 Prompt 对象
            Prompt prompt = new Prompt(messageList, chatOptions);
            // 调用大模型
            ChatResponse chatResponse = chatModel.call(new Prompt(messageList, chatOptions));
            // 如果有工具调用，则循环处理
            while (chatResponse.hasToolCalls()) {
                // 执行工具调用
                ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
                // 更新 Prompt 对象
                prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
                // 再次调用大模型
                chatResponse = chatModel.call(prompt);
            }
            // 返回大模型返回的内容
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            // 记录错误日志
            log.error("【大模型问答】 普通请求发生异常 用户输入参数:{} 工具:{} ", messageList, JsonUtil.encodeToString(tools), e);
            // 返回错误信息
            return "请求大模型发送异常,请稍后再次尝试";
        }
    }


    /**
     * 流试请求大模型
     *
     * @param chatRequestParams 聊天参数
     * @param messageList       问答列表
     * @param response          响应体
     * @return 流试返回大模型返回内容
     */
    private SseEmitter streamChat(ChatRequestParams chatRequestParams, List<Message> messageList, HttpServletResponse response) {
        // 设置响应类型和编码
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        // 创建 SseEmitter 对象，设置超时时间为 0，表示不超时
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        // 异步执行 consumerSseResponse 方法
        sseStreamConsumerThreadPool.execute(() -> consumerSseResponse(messageList, chatRequestParams, emitter));
        // 返回 SseEmitter 对象
        return emitter;
    }

    /**
     * 消费 Sse 响应
     *
     * @param messageList       消息列表
     * @param chatRequestParams 聊天参数
     * @param emitter           SseEmitter 对象
     */
    private void consumerSseResponse(List<Message> messageList, ChatRequestParams chatRequestParams, SseEmitter emitter) {
        List<FunctionCallback> tools = Lists.newArrayList();
        try {
            // 获取 FunctionCallback 列表
            tools = registryMcpSyncClientService.getFunctionCallbacks(chatRequestParams.getStream(), chatRequestParams.getAgentId(), chatRequestParams.getMcpConfigs());
            // 执行 doConsumerSseResponse 方法
            doConsumerSseResponse(messageList, chatRequestParams, tools, emitter);
        } catch (Exception e) {
            // 记录错误日志
            log.error("【大模型问答】 流试请求发生异常 用户输入参数:{} 工具:{} ", messageList, JsonUtil.encodeToString(tools), e);
            // 发送错误信息
            emitter.completeWithError(e);
        }
    }

    /**
     * 执行消费 Sse 响应
     *
     * @param messageList       消息列表
     * @param chatRequestParams 聊天参数
     * @param tools             FunctionCallback 列表
     * @param emitter           SseEmitter 对象
     */
    private void doConsumerSseResponse(List<Message> messageList, ChatRequestParams chatRequestParams, List<FunctionCallback> tools, SseEmitter emitter) {
        String agentId = chatRequestParams.getAgentId();
        // 创建 AtomicBoolean 对象，用于标记是否已完成
        AtomicBoolean completed = new AtomicBoolean(false);
        // 构建 ToolCallingChatOptions 对象
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder().toolCallbacks(tools).internalToolExecutionEnabled(chatRequestParams.getInternalToolExecutionEnabled()).build();
        // 创建 Prompt 数组
        Prompt[] prompt = {new Prompt(messageList, chatOptions)};
        // 用于存储最后一个 chatResponse
        final ChatResponse[] lastChatResponse = {null};
        // 调用大模型
        chatModel.stream(prompt[0])
                .subscribe(
                        streamChatEventHandler.createChatResponseConsumer(emitter, agentId, completed, prompt, chatOptions, lastChatResponse), // 注册 ChatResponse 处理器
                        streamChatEventHandler.createChaterrorConsumer(emitter, completed), // 注册 ChatError 处理器
                        streamChatEventHandler.createCompleteConsumerByFinishReasonStop(emitter, agentId, completed, lastChatResponse)); // 注册 Complete 处理器
    }
}
