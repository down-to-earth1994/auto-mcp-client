package com.pig4cloud.pig.mcp.client.chat;


import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ChatService {
    Object chat(ChatRequestParams chatRequestParams, HttpServletResponse response);
}
