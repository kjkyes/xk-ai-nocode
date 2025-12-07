package com.xk.xkainocode.ai;

import com.xk.xkainocode.ai.model.HtmlCodeResult;
import com.xk.xkainocode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 代码生成器服务
 */
public interface AiCodeGeneratorService {

    /**
     * 将代码生成为单html文件
     *
     * @param userPrompt 用户消息
     * @return ai生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(@UserMessage String userPrompt);

    /**
     * 将代码生成到多文件中
     *
     * @param userPrompt 用户消息
     * @return ai生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multiFile-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userPrompt);

    /**
     * 将代码生成为单html文件---流式输出
     *
     * @param userPrompt 用户消息
     * @return ai生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userPrompt);

    /**
     * 将代码生成到多文件中---流式输出
     *
     * @param userPrompt 用户消息
     * @return ai生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-multiFile-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userPrompt);

    /**
     * 生成 Vue项目代码---流式输出
     *
     * @param appId     项目id
     * @param userPrompt 用户消息
     * @return ai生成的代码
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-system-prompt.txt")
    TokenStream generateVueProjectStreaming(@MemoryId Long appId, @UserMessage String userPrompt);

}
