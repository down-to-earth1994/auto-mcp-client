package com.pig4cloud.pig.mcp.common.model;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "MCP工具描述信息")
public class McpToolInfo {

    @ApiModelProperty(value = "MCP工具名称")
    private String mcpClientName;
    @ApiModelProperty(value = "MCP SERVICE JSON SCHEMA")
    private McpSchema.ListToolsResult listToolsResult;

}
