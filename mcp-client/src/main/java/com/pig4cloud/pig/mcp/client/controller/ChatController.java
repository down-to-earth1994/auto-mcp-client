package com.pig4cloud.pig.mcp.client.controller;


import com.pig4cloud.pig.mcp.client.chat.ChatService;
import com.pig4cloud.pig.mcp.client.manager.ManagerMcpSyncClientService;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.client.resource.ResourceManagerService;
import com.pig4cloud.pig.mcp.client.tools.McpToolService;
import com.pig4cloud.pig.mcp.common.bean.RestResultCode;
import com.pig4cloud.pig.mcp.common.exception.RestCustomException;
import com.pig4cloud.pig.mcp.common.model.McpToolInfo;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.params.McpToolsParam;
import com.pig4cloud.pig.mcp.common.util.R;
import io.swagger.annotations.ApiParam;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chat")
@CrossOrigin("*")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private ResourceManagerService resourceManagerService;
    @Autowired
    private McpToolService mcpToolService;
    @Autowired
    private ManagerMcpSyncClientService managerMcpSyncClientService;

    @GetMapping("/check/mpc/online")
    public R checkMcpOnline(@ApiParam(value = "智能体id") String agentId, @ApiParam(value = "sse类型 clientName 是McpClientInfo.url stdio类型 是McpClientInfo.name") String clientName) {
        if (StringUtils.isBlank(agentId)) {
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "智能体id不能为空"));
        }
        if (StringUtils.isBlank(clientName)) {
            throw new RestCustomException(R.generic(RestResultCode.REST_COMMON_INVALID_PARAMETER.getCode(), "mcp客户端名称不能为空"));
        }
        return R.ok(mcpToolService.checkMcpOnline(agentId, clientName));
    }

    @GetMapping("/mpc/info")
    public R<ConcurrentHashMap<String, ConcurrentHashMap<String, McpClientInfo>>> mcpMemoryInfo() {
        return R.ok(managerMcpSyncClientService.allMcpClientMap());
    }

    @PostMapping("/multiple/mcp/tools/info")
    public R<List<McpToolInfo>> mcpToolsInfo(@RequestBody @Valid McpToolsParam mcpToolsParam) {
        return R.ok(mcpToolService.multipleMcpClientToolsResult(mcpToolsParam.getAgentId(), mcpToolsParam.getMcpConfig()));
    }

    @GetMapping("/default/mcp/info")
    public R<List<ChatRequestParams.McpConfig>> loadJson() {
        return R.ok(resourceManagerService.loadJson(ChatRequestParams.McpConfig.class));
    }

    @PostMapping("/ask")
    public Object ask(@RequestBody @Valid ChatRequestParams chatRequestParams, HttpServletResponse response) {
        return chatService.chat(chatRequestParams, response);
    }
}
