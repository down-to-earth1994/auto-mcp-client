package com.pig4cloud.pig.mcp.client.handle;

import cn.hutool.core.collection.CollectionUtil;
import com.pig4cloud.pig.mcp.client.enums.SseEventType;
import com.pig4cloud.pig.mcp.client.utils.SseEmitterHelpUtils;
import com.pig4cloud.pig.mcp.common.model.ChatResponseModel;
import com.pig4cloud.pig.mcp.common.util.JsonUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamChatEventHandlerImpl 类，用于处理流式聊天事件
 */
@Slf4j
@Service
public class StreamChatEventHandlerImpl implements StreamChatEventHandler {
    @Autowired
    private ChatModel chatModel; // 注入 ChatModel，用于与大模型进行交互

    @Autowired
    private ToolCallingManager toolCallingManager; // 注入 ToolCallingManager，用于处理工具调用

    @Resource(name = "sseStreamConsumerThreadPool")
    private ThreadPoolExecutor sseStreamConsumerThreadPool; // 注入线程池，用于异步执行任务


    /**
     * 处理 chatResponse 的核心逻辑
     *
     * @param emitter          SseEmitter 用于发送 SSE 事件
     * @param completed        AtomicBoolean 用于标记是否已完成
     * @param prompt           prompt数组，必须是长度为1的数组，用于在异步任务中更新 Prompt 对象
     * @param chatOptions      大模型聊天选项
     * @param lastChatResponse lastChatResponse 数组，必须是长度为1的数组，用于在 lambda 中修改外部变量
     * @return Consumer<ChatResponse> ChatResponse 处理器
     */
    @Override
    public Consumer<ChatResponse> createChatResponseConsumer(SseEmitter emitter, String agentId, AtomicBoolean completed, Prompt[] prompt, ToolCallingChatOptions chatOptions, ChatResponse[] lastChatResponse) {
        return chatResponse -> {
            // 存储最后一个 chatResponse，用于后续判断是否还有工具调用
            lastChatResponse[0] = chatResponse;
            // 判断 chatResponse 是否包含工具调用
            if (chatResponse.hasToolCalls()) {
                // 异步执行工具调用 防止在flux流中发生block()
                CompletableFuture.runAsync(() -> asyncTooCallThenAskLlm(emitter, agentId, completed, prompt, chatOptions, chatResponse), sseStreamConsumerThreadPool);
            } else {
                // 如果不包含工具调用，则发送助手内容
                String content = chatResponse.getResult().getOutput().getText();
                log.debug("【大模型流试输出】助手内容片段：{}", content);
                if (StringUtils.hasText(content)) {
                    SseEmitterHelpUtils.safeSend(emitter, SseEmitterHelpUtils.buildSseEvent(SseEventType.ASSISTANT, JsonUtil.encodeToString(new ChatResponseModel(content))), completed);
                }
            }
        };
    }

    /**
     * 异步执行工具调用，然后向大模型提问
     *
     * @param emitter      SseEmitter 用于发送 SSE 事件
     * @param completed    AtomicBoolean 用于标记是否已完成
     * @param prompt       prompt数组，必须是长度为1的数组，用于在异步任务中更新 Prompt 对象
     * @param chatOptions  大模型聊天选项
     * @param chatResponse ChatResponse 对象
     */
    private void asyncTooCallThenAskLlm(SseEmitter emitter, String agentId, AtomicBoolean completed, Prompt[] prompt, ToolCallingChatOptions chatOptions, ChatResponse chatResponse) {
        Prompt currentPrompt = prompt[0]; // 复制 prompt
        try {
            // TODO 待优化 大模型返回的 tool args json JsonParseException异常 导致无法进行工具调用 也无法将错误json输送给大模型让其整理
            // 如果包含工具调用，则发送工具参数
            sendToolArgs(emitter, chatResponse, completed);
            // 调用工具失败 异常会自动追加到Message 大模型整理
            ToolExecutionResult result = getToolExecutionResult(chatResponse, currentPrompt); // 获取工具执行结果
            List<Message> newMessages = result.conversationHistory(); // 获取新的消息列表
            sendToolResult(emitter, result, result.conversationHistory(), completed); // 发送工具结果
            currentPrompt = new Prompt(newMessages, chatOptions); // 创建新的 prompt
            // 用于存储最后一个 chatResponse
            final ChatResponse[] lastChatResponse = {chatResponse};
            chatModel.stream(currentPrompt)
                    .subscribe(
                            createChatResponseConsumer(emitter, agentId, completed, new Prompt[]{currentPrompt}, chatOptions, lastChatResponse),
                            createChaterrorConsumer(emitter, completed),
                            createCompleteConsumerByFinishReasonStop(emitter, agentId, completed, lastChatResponse));
        } catch (Exception e) {
            log.error("【大模型流试输出】调用工具执行失败", e);
            SseEmitterHelpUtils.safeError(emitter, e, completed); // 发送错误信息
        }
    }

    /**
     * 获取工具执行结果
     *
     * @param chatResponse  ChatResponse 对象
     * @param currentPrompt 当前 Prompt 对象
     * @return ToolExecutionResult 工具执行结果
     */
    private ToolExecutionResult getToolExecutionResult(ChatResponse chatResponse, Prompt currentPrompt) {
        return toolCallingManager.executeToolCalls(currentPrompt, chatResponse);
    }

    /**
     * 创建 ChatError 处理器
     *
     * @param emitter   SseEmitter 用于发送 SSE 事件
     * @param completed AtomicBoolean 用于标记是否已完成
     * @return Consumer<? super Throwable> 错误处理器
     */
    @Override
    public Consumer<? super Throwable> createChaterrorConsumer(SseEmitter emitter, AtomicBoolean completed) {
        return error -> {
            log.error("【大模型流试输出】 消费大模型返回的流试片段异常", error);
            SseEmitterHelpUtils.safeError(emitter, error, completed); // 发送错误信息
        };
    }

    /**
     * 创建 Complete 处理器，用于处理 FinishReason 为 STOP 的情况
     *
     * @param emitter          SseEmitter 用于发送 SSE 事件
     * @param completed        AtomicBoolean 用于标记是否已完成
     * @param lastChatResponse lastChatResponse 数组，必须是长度为1的数组，用于在 lambda 中修改外部变量
     * @return Runnable 完成处理器
     */
    @Override
    public Runnable createCompleteConsumerByFinishReasonStop(SseEmitter emitter, String agentId, AtomicBoolean completed, ChatResponse[] lastChatResponse) {
        return () -> {
            HashSet<String> finishReason = getLlmFinishReason();
            // 判断是否是由于 STOP 结束
            if (lastChatResponse != null && lastChatResponse[0] != null && lastChatResponse[0].hasFinishReasons(finishReason)) {
                log.debug("【大模型流试输出】 智能体id:{} 问答结束 结束标志:{},SseEmitter关闭", agentId, lastChatResponse[0].getResult().getMetadata().getFinishReason());
                SseEmitterHelpUtils.safeComplete(emitter, completed); // 完成 SseEmitter
            }
        };
    }


    private HashSet<String> getLlmFinishReason() {
        // 判断是否是由于 TOOL_CALLS 结束
        HashSet<String> finishReason = new HashSet();
        finishReason.add("STOP");
        return finishReason;
    }

    /**
     * 发送工具参数
     *
     * @param emitter      SseEmitter 用于发送 SSE 事件
     * @param chatResponse ChatResponse 对象
     * @param completed    AtomicBoolean 用于标记是否已完成
     */
    private void sendToolArgs(SseEmitter emitter, ChatResponse chatResponse, AtomicBoolean completed) {
        List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResult().getOutput().getToolCalls(); // 获取工具调用列表
        if (!CollectionUtils.isEmpty(toolCalls)) {
            AssistantMessage.ToolCall toolCall = toolCalls.get(0); // 获取第一个工具调用
            ToolRequest toolRequest = new ToolRequest(toolCall.name(), toolCall.arguments()); // 创建 ToolRequest 对象
            log.debug("【大模型流试输出】命中工具详情: {}", JsonUtil.encodeToString(toolRequest));
            SseEmitter.SseEventBuilder sseEventBuilder = SseEmitterHelpUtils.buildSseEvent(SseEventType.TOOL_ARGS, JsonUtil.encodeToString(toolRequest)); // 创建 SSE 事件
            SseEmitterHelpUtils.safeSend(emitter, sseEventBuilder, completed); // 发送 SSE 事件
        }
    }

    /**
     * 发送工具结果
     *
     * @param emitter     SseEmitter 用于发送 SSE 事件
     * @param result      ToolExecutionResult 工具执行结果
     * @param newMessages 新的消息列表
     * @param completed   AtomicBoolean 用于标记是否已完成
     */
    private void sendToolResult(SseEmitter emitter, ToolExecutionResult result, List<Message> newMessages, AtomicBoolean completed) {
        if (CollectionUtil.isNotEmpty(newMessages)) {
            log.debug("【大模型流试输出】工具调用结果：{}", result);
            Message message = newMessages.get(newMessages.size() - 1); // 获取最后一条消息
            if (MessageType.TOOL.equals(message.getMessageType())) { // 判断消息类型是否为 TOOL
                List<ToolResponseMessage.ToolResponse> responses = ((ToolResponseMessage) message).getResponses(); // 获取工具响应列表
                List<String> allResponses = responses.stream().map(ToolResponseMessage.ToolResponse::responseData).toList(); // 提取所有响应数据
                SseEmitter.SseEventBuilder sseEventBuilder = SseEmitterHelpUtils.buildSseEvent(SseEventType.TOOL_RESULT, JsonUtil.encodeToString(allResponses)); // 创建 SSE 事件
                SseEmitterHelpUtils.safeSend(emitter, sseEventBuilder, completed); // 发送 SSE 事件
            }
        }
    }

    /**
     * ToolRequest 记录，用于封装工具请求信息
     *
     * @param toolName  工具方法名
     * @param arguments 工具参数
     */
    private record ToolRequest(String toolName, String arguments) {
    }
}
