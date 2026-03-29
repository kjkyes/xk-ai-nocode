package com.xk.xkainocode.ai.agent.tool;


import dev.langchain4j.agent.tool.Tool;

public class TerminateTool {
  
    @Tool("Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task. " +
            "When you have finished all the tasks, call this tool to end the work.")
    public String doTerminate() {  
        return "任务结束";  
    }  
}
