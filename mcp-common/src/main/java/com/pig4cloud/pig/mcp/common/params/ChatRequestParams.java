package com.pig4cloud.pig.mcp.common.params;

import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.util.R;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@ApiModel(description = "大模型问答参数")
@Getter
@Setter
public class ChatRequestParams {
    @ApiModelProperty("模型名称")
    @NotBlank(message = "模型名称不能为空")
    private String model;

    @ApiModelProperty("智能体ID")
    @NotBlank(message = "智能体ID不能为空")
    private String agentId;

    @ApiModelProperty("对话消息")
    @NotEmpty(message = "对话消息不能为空")
    private List<Message> messages;

    @ApiModelProperty("MCP Server配置")
    private List<McpConfig> mcpConfigs;

    @ApiModelProperty("默认不是流试返回")
    private Boolean stream = false;

    @ApiModelProperty("mcp工具执行是否框架自动管理")
    private Boolean internalToolExecutionEnabled = false;

    //defaultFlag 是否内置 0 内置 1 自主添加
    public record McpConfig(String name, String desc, String type, Boolean defaultFlag, String url, String command,
                            List<String> args,
                            Map<String, String> env) {
    }

    ;

    public record Message(String role, String content) {
    }

    ;

    public List<org.springframework.ai.chat.messages.Message> transMessage() {
        List<org.springframework.ai.chat.messages.Message> messagesCopy = new ArrayList<>();
        this.messages.forEach(message -> {
            if (MessageType.USER.getValue().equals(message.role)) {
                messagesCopy.add(new UserMessage(message.content()));
            } else if (MessageType.SYSTEM.getValue().equals(message.role)) {
                messagesCopy.add(new SystemMessage(message.content()));
            } else if (MessageType.ASSISTANT.getValue().equals(message.role)) {
                messagesCopy.add(new SystemMessage(message.content()));
            } else {
                throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "消息角色错误"));
            }
        });
        return messagesCopy;
    }
}
