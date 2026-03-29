package com.xk.xkainocode.ai.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xk 的超级智能体实例（拥有自主规划能力，可以直接使用）
 */
@Component
public class XKManus extends ToolCallAgent {

    public XKManus(List<ToolSpecification> allTools) {
        super(allTools);
        this.setName("xkManus");
        String SYSTEM_PROMPT = """  
                You are xkManus, an all-capable AI assistant, aimed at solving any task presented by the user.  
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.  
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxStep(20);
    }
}
