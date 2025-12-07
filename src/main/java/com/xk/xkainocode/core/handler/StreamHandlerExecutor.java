package com.xk.xkainocode.core.handler;

import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import com.xk.xkainocode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 创建流处理器并处理聊天历史记录
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @param codeGenType        代码生成类型
     * @return 处理后的流
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  Long appId,
                                  User loginUser,
                                  CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT ->
                jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE ->
                new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
