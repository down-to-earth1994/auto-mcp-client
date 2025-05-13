package com.pig4cloud.pig.mcp.client.consts;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class LlmVarConsts {
    public static ThreadLocal<SseEmitter> sseThreadLocal = new ThreadLocal<>();

    public static ThreadLocal<Object> handleThreadLocal = new ThreadLocal<>();

}
