package com.pig4cloud.pig.mcp.client.chat;

import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.alibaba.nacos.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import com.pig4cloud.pig.mcp.client.enums.McpClientType;
import com.pig4cloud.pig.mcp.client.manager.model.McpClientInfo;
import com.pig4cloud.pig.mcp.client.manager.task.McpCommonTask;
import com.pig4cloud.pig.mcp.common.params.ChatRequestParams;
import com.pig4cloud.pig.mcp.common.util.HttpSseUrlUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 测试aicp V10 llm 大模型效果
 */
public class CustomerChatModel {

    private final String BASE_URL = "http://10.0.1.133:30080";

    public static void main(String[] args) {
        CustomerChatModel chatModel = new CustomerChatModel();
        List<Message> propmt = chatModel.getPropmt();
        chatModel.callQwen330b(propmt);
//        chatModel.callQwen2572b(propmt);
    }

    public List<Message> getPropmt() {
        List<Message> propmt = Lists.newArrayList();
        propmt.add(new SystemMessage("使用中文回答"));
        propmt.add(new UserMessage("北京和上海哪个天气更热展示图表"));
        return propmt;
    }


    //调用qwen3-30b-a3b模型
    public void callQwen330b(List<Message> propmt) {
        String apiKey = "YWljcF9hcHA6UVd4aFpHUnBianB2Y0dWdUlITmxjMkZ0WlE=";
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(BASE_URL)
                .apiKey(apiKey)
                .completionsPath("/v10/llm/chat/qwen3-30b-a3b/completion")
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().openAiApi(openAiApi).build();

        List<McpSyncClient> syncClientList = getMcpSyncClients();
        ToolCallbackProvider[] mcpTools = {new CustomSyncMcpToolCallbackProvider(syncClientList)};
        List<FunctionCallback> tools = Arrays.stream(mcpTools) // 将数组转换为 Stream
                .filter(Objects::nonNull) // 过滤掉 null 元素
                .map(ToolCallbackProvider::getToolCallbacks) // 获取 FunctionCallback 数组
                .filter(Objects::nonNull) // 过滤掉 null 数组
                .flatMap(Arrays::stream) // 将 FunctionCallback 数组转换为 Stream
                .collect(Collectors.toList());
//        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder().toolCallbacks(tools).internalToolExecutionEnabled(true).build();
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().toolCallbacks(tools).toolChoice(OpenAiApi.ChatCompletionRequest.ToolChoiceBuilder.AUTO).topP(0.1D).internalToolExecutionEnabled(true).build();
        // 创建 Prompt 对象
        Prompt prompt = new Prompt(propmt, chatOptions);
        chatModel.stream(prompt).subscribe(
                chatResponse -> {
                    if (chatResponse.hasToolCalls()) {
                        System.out.println("chatResponse = " + chatResponse);
                        AssistantMessage.ToolCall toolCall = chatResponse.getResult().getOutput().getToolCalls().get(0);
                        String name = toolCall.name();
                        String type = toolCall.type();
                        String id = toolCall.id();
                        String arguments = toolCall.arguments();
                        System.out.println("name = " + name);
                        System.out.println("type = " + type);
                        System.out.println("id = " + id);
                        System.out.println("arguments = " + arguments);
                    }
                }
        );
//        String text = call.getResult().getOutput().getText();
//        System.out.println("Qwen3 = " + text);
    }

    @NotNull
    private static List<McpSyncClient> getMcpSyncClients() {
        McpSyncClient amapSyncClient = getHttpSseAmapClient();
        McpSyncClient quickChartMcpClient = getQuickChartMcpServer();
        List<McpSyncClient> syncClientList = Lists.newArrayList();
        syncClientList.add(amapSyncClient);
        syncClientList.add(quickChartMcpClient);
        return syncClientList;
    }

    @NotNull
    private static McpSyncClient getHttpSseAmapClient() {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("https://mcp.amap.com").sseEndpoint("/sse?key=79b3a68afd8cd4d81341e6c7aa41eddf").build();
        McpSyncClient amapSyncClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
        amapSyncClient.initialize();
        return amapSyncClient;
    }

    private static McpSyncClient getQuickChartMcpServer() {
        String commandPath = "C:\\nvm4w\\nodejs\\npx.cmd";
        String[] args = {"-y", "@gongrzhe/quickchart-mcp-server"};
        Map<String, String> env = new HashMap<>();
        ServerParameters serverParameters = ServerParameters.builder(commandPath).args(args).env(env).build();
        StdioClientTransport stdioClientTransport = new StdioClientTransport(serverParameters);
        McpSyncClient stdioMcpSyncClient = McpClient.sync(stdioClientTransport).requestTimeout(Duration.ofSeconds(30)).build();
        stdioMcpSyncClient.initialize();
        return stdioMcpSyncClient;
    }


    //调用qwen25-72b-instruct模型
    public void callQwen2572b(List<Message> propmt) {
        String apiKey = "Y2EzMzE5OWIzNWMxYWI3YmZjYzc2MWU2NjU2NGRjMDMgIC0K";
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(BASE_URL)
                .apiKey(apiKey)
                .completionsPath("/v10/llm/chat/qwen25-72b-instruct/completion")
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().openAiApi(openAiApi).build();
        ChatResponse call = chatModel.call(new Prompt(propmt));
        String text = call.getResult().getOutput().getText();
        System.out.println("Qwen3 = " + text);
    }


}
