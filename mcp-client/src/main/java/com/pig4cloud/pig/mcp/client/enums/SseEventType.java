package com.pig4cloud.pig.mcp.client.enums;

import lombok.Getter;

@Getter
public enum SseEventType {
    TOOL_ARGS("tool_args", "MCP工具参数"),
    TOOL_RESULT("tool_result", "MCP工具调用结果"),
    ASSISTANT("assistant", "助手消息"),
    ;

    private String name;
    private String description;

    SseEventType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
