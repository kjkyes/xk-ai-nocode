package com.xk.xkainocode.ai.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * ReAct（Reasoning and Acting）模式的 agent抽象类
 * 实现了思考-行动的循环模式
 */
public abstract class ReActAgent extends BaseAgent {

    /**
     * 工具调用请求
     */
    private List<ToolExecutionRequest> toolRequests = new ArrayList<>();

    /**
     * 思考方法，根据当前状态和消息上下文决定是否进行下一步的行动
     *
     * @return true表示需要继续行动，false表示结束行动
     */
    public abstract boolean think(List<ToolExecutionRequest> toolRequests);

    /**
     * 行动方法，根据当前状态和消息上下文执行相应的操作
     *
     * @return 操作的结果
     */
    public abstract String act(List<ToolExecutionRequest> toolRequests);

    @Override
    public String step() {
        try {
            if (think(toolRequests)) {
                return act(toolRequests);
            }else {
                return "思考完成，无需额外行动";
            }
        } catch (Exception e) {
            e.getStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
