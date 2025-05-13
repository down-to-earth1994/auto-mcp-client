package com.pig4cloud.pig.mcp.client.enums;

import lombok.Getter;

@Getter
public enum McpClientType {
    SSE("sse"),
    STDIO("stdio");

    private String name;


    McpClientType(String name) {
        this.name = name;

    }
}
