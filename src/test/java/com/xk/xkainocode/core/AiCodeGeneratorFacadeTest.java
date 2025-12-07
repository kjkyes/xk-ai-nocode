package com.xk.xkainocode.core;

import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

//    @Test
//    void generateAndSaveCode() {
//        File result = aiCodeGeneratorFacade.generateAndSave("请给我生成一个登录页面", CodeGenTypeEnum.HTML, 1L);
//        assertNotNull(result);
//    }

//    @Test
//    void generateAndSaveStream() {
//        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveStream("请给我生成个人博客网站,不要增加额外的字段", CodeGenTypeEnum.MULTI_FILE, 342543098704412672L);
//        // 阻塞等待所有数据收集完成
//        List<String> result = codeStream.collectList().block();
//        assertNotNull(result);
//        String completeCode = String.join("", result);
//        assertNotNull(completeCode);
//    }

//    @Test
//    void generateAndSaveVuepProjectStream() {
//        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveStream("请给我生成任务记录网站，代码300行左右", CodeGenTypeEnum.VUE_PROJECT, 0L);
//        // 阻塞等待所有数据收集完成
//        List<String> result = codeStream.collectList().block();
//        assertNotNull(result);
//        String completeCode = String.join("", result);
//        assertNotNull(completeCode);
//    }

    @Test
    void generateAiTest(){
        Flux<String> resultStream = aiCodeGeneratorFacade.generateAndSaveStream("请帮我创建一个现代化的个人博客网站，仅包含联系作者页面。采用简洁的设计风格，支持响应式布局，首页展示作者简介。", CodeGenTypeEnum.VUE_PROJECT, 6L);
        List<String> result = resultStream.collectList().block();
        assertNotNull(result);
        String completeCode = String.join("", result);
        assertNotNull(completeCode);
    }
}