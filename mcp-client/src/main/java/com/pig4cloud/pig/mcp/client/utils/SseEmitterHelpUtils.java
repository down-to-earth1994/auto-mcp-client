package com.pig4cloud.pig.mcp.client.utils;

import com.pig4cloud.pig.mcp.client.enums.SseEventType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * SseEmitterHelpUtils 类，用于提供 SseEmitter 的辅助方法
 */
@Slf4j
public class SseEmitterHelpUtils {
    /**
     * 构建 SseEvent 对象
     *
     * @param eventType SseEventType 枚举类型，表示事件类型
     * @param data      String 类型，表示事件数据
     * @return SseEmitter.SseEventBuilder 对象
     */
    public static SseEmitter.SseEventBuilder buildSseEvent(SseEventType eventType, String data) {
        return SseEmitter.event()
                .name(eventType.getName())
                .data(data);
    }

    /**
     * 安全地发送 SSE 事件
     *
     * @param emitter   SseEmitter 对象，用于发送 SSE 事件
     * @param event     SseEmitter.SseEventBuilder 对象，表示 SSE 事件
     * @param completed AtomicBoolean 对象，用于标记是否已完成
     */
    public static void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder event, AtomicBoolean completed) {
        try {
            if (!completed.get()) {
                emitter.send(event);
            }
        } catch (IOException e) {
            //如果emitter已关闭再次发送进行异常捕获并打印
            log.error("【SSE Help Util】发送Data失败： event:{}", event, e);
            safeError(emitter, e, completed);
        }
    }

    /**
     * 安全地完成 SseEmitter
     *
     * @param emitter   SseEmitter 对象，用于发送 SSE 事件
     * @param completed AtomicBoolean 对象，用于标记是否已完成
     */
    public static void safeComplete(SseEmitter emitter, AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("【SSE Help Util】关闭 SSE 失败", e);
            }
        }
    }

    /**
     * 安全地发送错误信息
     *
     * @param emitter   SseEmitter 对象，用于发送 SSE 事件
     * @param error     Throwable 对象，表示错误信息
     * @param completed AtomicBoolean 对象，用于标记是否已完成
     */
    public static void safeError(SseEmitter emitter, Throwable error, AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            try {
                emitter.completeWithError(error);
            } catch (Exception e) {
                log.error("【SSE Help Util】发送Data出现异常", e);
            }
        }
    }
}
