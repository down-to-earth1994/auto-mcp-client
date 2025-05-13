package com.pig4cloud.pig.mcp.client.handle;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface StreamChatEventHandler {


    Consumer<ChatResponse> createChatResponseConsumer(SseEmitter emitter, String agentId,AtomicBoolean completed, Prompt[] prompt, ToolCallingChatOptions chatOptions, ChatResponse[] lastChatResponse);

    Consumer<? super Throwable> createChaterrorConsumer(SseEmitter emitter, AtomicBoolean completed);

    Runnable createCompleteConsumerByFinishReasonStop(SseEmitter emitter, String agentId, AtomicBoolean completed, ChatResponse[] lastChatResponse);

}
