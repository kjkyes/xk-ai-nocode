package com.xk.xkainocode.langGraph4j.node;

import com.xk.xkainocode.ai.AiCodeGenTypeRoutingService;
import com.xk.xkainocode.ai.AiCodeGenTypeRoutingServiceFactory;
import com.xk.xkainocode.langGraph4j.state.WorkflowContext;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import com.xk.xkainocode.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 智能路由节点
 *
 * 传入状态：原始提示词
 * 更新状态：当前执行步骤、代码生成类型
 */
@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务
                // 获取AI路由服务工厂并创建新的路由服务实例
                AiCodeGenTypeRoutingServiceFactory factory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService routingService = factory.createAiCodeGenTypeRoutingService();
                // 根据原始提示词进行智能路由
                // todo 注意这里因为选用chatglm模型，输出包含<think>，我们在此临时处理一下
                String row = routingService.routeCodeGenType(context.getOriginalPrompt());
                String clean = row.replaceAll("(?s)<think>.*?</think>", "").trim();
                generationType = CodeGenTypeEnum.valueOf(clean);

                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}

