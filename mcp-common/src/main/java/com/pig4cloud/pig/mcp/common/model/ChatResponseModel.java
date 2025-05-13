package com.pig4cloud.pig.mcp.common.model;

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
@ApiModel(value = "大模型返回内容")
public class ChatResponseModel {

    @ApiModelProperty(value = "大模型返回文本信息")
    private String content;


}
