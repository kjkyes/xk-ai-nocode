package com.xk.xkainocode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xk.xkainocode.ai.guardrail.PromptSafetyInputGuardrail;
import com.xk.xkainocode.ai.tools.ToolManager;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import com.xk.xkainocode.service.ChatHistoryService;
import com.xk.xkainocode.util.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取普通模型服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 缓存键 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = createCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例
     * 根据生成代码类型创建对应的 AiService
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(@MemoryId long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                // 将对话历史保存在redis
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(80)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return switch (codeGenType) {
            // 生成 Vue工程项目，使用工具调用和推理模型
            case VUE_PROJECT -> {
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        // 当 AI 出现幻觉，调用了不存在的工具时
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called"
                                        + toolExecutionRequest.name()))
                        .maxSequentialToolsInvocations(20)  // 最多连续调用 20 次工具
//                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 输入护轨
//                        .outputGuardrails(new RetryOutputGuardrail()) // 输出护轨
                        .build();
            }
            case HTML, MULTI_FILE -> {
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 输入护轨
                        .build();
            }
        };
    }

    /**
     * 创建 AI 代码生成器服务
     * 包括结构化输出和流式输出
     * <p>
     * 这里使用了内置隔离机制，在保持使用同一个AiService的前提下，根据预设 id 将不同对话记忆隔离开
     *
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 为了跟AiService隔离机制兼容，这里我们将其余不要对话记忆的业务统一分配唯一的AiService
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 根据 appId和代码生成类型为缓存生成缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String createCacheKey(Long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
