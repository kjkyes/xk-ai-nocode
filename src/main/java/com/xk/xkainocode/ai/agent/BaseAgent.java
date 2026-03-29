package com.xk.xkainocode.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.ai.agent.model.AgentState;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 智能体基类
 */
@Data
@Slf4j
public abstract class BaseAgent {
    // 核心属性
    private String name;

    private Long appId;
    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态 (默认空闲状态)
    private AgentState agentState = AgentState.IDLE;

    // 执行步骤数控制
    private int currentStep = 0;
    private int maxStep = 10;

    // Memory (用于维护会话上下文)
    private List<ChatMessage> messageList = new ArrayList<>();

    /**
     * 同步运行 agent
     *
     * @return 执行结果
     * @Param userPrompt 用户输入的提示词
     */
    public String run(String userPrompt) {
        // 基础校验
        if(agentState != AgentState.IDLE) {
            throw new RuntimeException("Connot run agent in state: " + agentState);
        }
        if(StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("User prompt cannot be empty");
        }
        // 更改执行状态
        agentState = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for(int i = currentStep; i < maxStep && agentState != AgentState.FINISHED; i++) {
                int stepNum = i + 1;
                currentStep = stepNum;
                log.info("Executing step: {}", stepNum);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNum + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤数限制
            if(currentStep >= maxStep) {
                agentState = AgentState.FINISHED;
                results.add("Terminated: Reach the maxStep (" + maxStep + ")");
            }

            return String.join("\n", results);
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Error occurred while running agent", e);
            return "执行错误: " + e.getMessage();
        }finally {
            // 清理资源
            cleanup();
        }
    }

    /**
     * SSE方式运行 agent
     *
     * @return 执行结果
     * @Param userPrompt 用户输入的提示词
     */
    public SseEmitter runSSE(String userPrompt) {
        // 创建一个超时时间较长的SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L);
        // 设置线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try{
                // 基础校验
                if(agentState != AgentState.IDLE) {
                    sseEmitter.send("Cannot run agent in state: " + agentState);
                    sseEmitter.complete();
                    return;
                }
                if(StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("Cannot run agent with none prompt");
                    sseEmitter.complete();
                    return;
                }
                // 更改执行状态
                agentState = AgentState.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));
                // 保存结果列表
                List<String> results = new ArrayList<>();
                try {
                    // 执行循环
                    for(int i = currentStep; i < maxStep && agentState != AgentState.FINISHED; i++) {
                        int stepNum = i + 1;
                        currentStep = stepNum;
                        log.info("Executing step: {}", stepNum);
                        // 单步执行
                        String stepResult = step();
                        String result = "Step " + stepNum + ": " + stepResult;
                        results.add(result);
                        sseEmitter.send(result);
                    }
                    // 检查是否超出步骤数限制
                    if(currentStep >= maxStep) {
                        agentState = AgentState.FINISHED;
                        sseEmitter.send("Terminated: Reach the maxStep (" + maxStep + ")");
                    }
                } catch (Exception e) {
                    agentState = AgentState.ERROR;
                    log.error("Error occurred while running agent", e);
                    try {
                        sseEmitter.send("执行错误: " + e.getMessage());
                    } catch (IOException ex) {
                        sseEmitter.completeWithError(ex);
                    }
                    sseEmitter.complete();
                }finally {
                    // 清理资源
                    cleanup();
                }
            }catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            agentState = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if(agentState == AgentState.RUNNING) {
                agentState = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    /**
     * 定义单个步骤的内容
     *
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}
