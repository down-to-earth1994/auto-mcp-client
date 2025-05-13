package com.pig4cloud.pig.mcp.common.params;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel(description = "获取MCP定义的工具信息")
public class McpToolsParam {
    @ApiModelProperty("智能体id")
    @NotBlank(message = "智能体id不能为空")
    private String agentId;

    @ApiModelProperty("MCP工具信息")
    @NotEmpty(message = "MCP工具信息不能为空")
    private List<ChatRequestParams.McpConfig> mcpConfig;
}

