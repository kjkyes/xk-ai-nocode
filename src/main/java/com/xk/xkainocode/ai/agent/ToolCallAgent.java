package com.xk.xkainocode.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.ai.agent.model.AgentState;
import com.xk.xkainocode.ai.agent.tool.*;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理工具调用的 基础 agent类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Value("${search-api.api_key}")
    private String apiKey;

    // 可用的工具
    private final List<ToolSpecification> availableTools;

    // 保存工具调用信息的响应结果（要调用哪些工具）
    private AiMessage toolCallResponse;

    static Map<String, ToolExecutor> executors = new HashMap<>();

    public ToolCallAgent(List<ToolSpecification> availableTools) {
        // 调用下父组件的构造函数
        super();
        this.availableTools = availableTools;
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think(List<ToolExecutionRequest> toolRequests) {
        // 1.校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            SystemMessage systemMessage = new SystemMessage(getNextStepPrompt());
            getMessageList().add(systemMessage);
        }
        try {
            // 构造请求
            ChatRequest request = ChatRequest.builder()
                    .messages(getMessageList())
                    .toolSpecifications(availableTools)
                    .build();
            // 2.调用 AI 大模型，获取工具调用结果
            ChatResponse response = chatModel.chat(request);

            // 3.解析工具调用结果，获取要调用的工具
            // 助手消息
            AiMessage aiMessage = response.aiMessage();
            // 记录响应，稍后用于act()
            toolCallResponse = aiMessage;
            getMessageList().add(aiMessage);
            // 检查是否有工具调用请求
            List<ToolExecutionRequest> requests = aiMessage.hasToolExecutionRequests()
                    ? aiMessage.toolExecutionRequests()
                    : new ArrayList<>();
            // 输出提示信息
            String result = aiMessage.text();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了" + requests.size() + "个工具来使用");

            // 如果不需要调用工具，返回 false
            if (!requests.isEmpty()) {
                String toolCallInfo = requests.stream()
                        .map(toolCall -> String.format("工具名称：%s，工具参数：%s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(getName() + "调用的工具信息如下：\n" + toolCallInfo);
                // 更新最新的工具调用请求
//                toolRequests.clear();
                toolRequests.addAll(requests);
            }
            return !requests.isEmpty();
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AiMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }

    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行行动的结果
     */
    @Override
    public String act(List<ToolExecutionRequest> toolRequests) {
        // 校验工具响应是否包含需要调用的工具
        if (toolCallResponse == null || !toolCallResponse.hasToolExecutionRequests()) {
            return "不需要调用工具";
        }
        // 判断是否调用了终止工具
        toolRequests = toolCallResponse.hasToolExecutionRequests()
                ? toolCallResponse.toolExecutionRequests()
                : new ArrayList<>();
        boolean doTerminateToolCalled = toolRequests.stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (doTerminateToolCalled) {
            setAgentState(AgentState.FINISHED);
        }
        // 调用工具
        // 6. 检查并执行工具调用
        executors = getExecutors();
        StringBuilder res = new StringBuilder();
        for (ToolExecutionRequest toolRequest : toolCallResponse.toolExecutionRequests()) {
            String toolName = toolRequest.name();
            ToolExecutor executor = executors.get(toolName);
            if (executor != null) {
                try {
                    // 直接使用 DefaultToolExecutor 执行，自动调用工具类方法
                    String result = executor.execute(toolRequest, null);
                    res.append("工具：").append(toolName).append("调用成功，结果为：").append(result).append("\n");
                    getMessageList().add(ToolExecutionResultMessage.from(toolRequest, result));
                } catch (Exception e) {
                    log.error("工具执行失败: {}", toolName, e);
                    String errorMsg = "执行失败: " + e.getMessage();
                    res.append("工具：").append(toolName).append("调用失败，结果为：").append(errorMsg).append("\n");

                    // 即使失败也要返回错误信息
                    getMessageList().add(ToolExecutionResultMessage.from(toolRequest, errorMsg));
                }
            } else {
                String errorMsg = "未找到工具: " + toolName;
                res.append(errorMsg).append("\n");
                getMessageList().add(ToolExecutionResultMessage.from(toolRequest, errorMsg));
            }
        }
        return res.toString();
    }

    /**
     * 构建工具执行器
     * @param toolInstance
     * @return
     */
    private Map<String, ToolExecutor> buildExecutors(Object toolInstance, Map<String, ToolExecutor> executorMap) {

        Method[] methods = toolInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                String toolName = method.getAnnotation(Tool.class).name();
                if (toolName.isEmpty()) {
                    toolName = method.getName();
                }
                executorMap.put(toolName, new DefaultToolExecutor(toolInstance, method));
            }
        }
        return executorMap;
    }

    /**
     * 获取工具执行器
     * @return
     */
    private Map<String, ToolExecutor> getExecutors() {
        Map<String, ToolExecutor> executors = new HashMap<>();
        buildExecutors(new FileOperationTool(), executors);
        buildExecutors(new PDFGenerationTool(), executors);
        buildExecutors(new ResourceDownloadTool(), executors);
        buildExecutors(new TerminalOperationTool(), executors);
        buildExecutors(new TerminateTool(), executors);
        buildExecutors(new WebScrapTool(), executors);
        buildExecutors(new WebSearchTool(apiKey), executors);
        return executors;
    }
}
