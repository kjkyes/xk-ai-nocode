package com.xk.xkainocode.core;

import cn.hutool.json.JSONUtil;
import com.xk.xkainocode.ai.AiCodeGeneratorService;
import com.xk.xkainocode.ai.AiCodeGeneratorServiceFactory;
import com.xk.xkainocode.ai.model.HtmlCodeResult;
import com.xk.xkainocode.ai.model.MultiFileCodeResult;
import com.xk.xkainocode.ai.model.message.AiResponseMessage;
import com.xk.xkainocode.ai.model.message.ToolExecutedMessage;
import com.xk.xkainocode.ai.model.message.ToolRequestMessage;
import com.xk.xkainocode.constant.AppConstant;
import com.xk.xkainocode.core.builder.VueProjectBuilder;
import com.xk.xkainocode.core.parser.CodeParserExecutor;
import com.xk.xkainocode.core.saver.CodeFileSaverExecutor;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面类，组合生成和保存代码功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage       用户消息
     * @param codeGenTypeEnum   生成类型
     * @param appId 应用 ID
     * @return 保存的目录
     */
    public File generateAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSave(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSave(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口，根据类型生成并保存代码（流式输出）
     *
     * @param userMessage       用户消息
     * @param codeGenTypeEnum   生成类型
     * @param appId 应用 ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT ->{
                TokenStream result = aiCodeGeneratorService.generateVueProjectStreaming(appId, userMessage);
                yield processTokenStreamToFlux(result, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * （适配器模式的思想）
     * 这里还可以通过tokenStream的其他回调方法去展示更多内容，例如部分思考内容，由此实现深度思考功能
     *
     * 另外，自 langchain4j的1.3.0版本修复了流式输出工具调用信息方法的参数不能有空格的问题，并且补充了 beforeToolExecution方法
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStreamToFlux(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }



    /**
     * 通用的流式代码解析并保存（流式）
     * @param codeStream      代码流
     * @param codeGenTypeEnum 代码生成类型
     * @param appId 应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        StringBuilder builder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 拼接输出的代码块
            builder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = builder.toString();
                // 使用解析器解析生成的代码为对象
                Object parsedCode = CodeParserExecutor.executeParse(completeCode, codeGenTypeEnum);
                // 保存生成的代码
                File file = CodeFileSaverExecutor.executeSave(parsedCode, codeGenTypeEnum, appId);
                log.info("保存的目录为:{}", file.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存" + codeGenTypeEnum.getValue() + "代码失败", e.getMessage());
            }
        });
    }

}
