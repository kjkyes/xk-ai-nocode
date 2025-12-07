//package com.xk.xkainocode.ai;
//
//import com.xk.xkainocode.ai.model.HtmlCodeResult;
//import com.xk.xkainocode.ai.model.MultiFileCodeResult;
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class AiCodeGeneratorServiceTest {
//
//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;
//
//    @Test
//    void generateHtmlCode() {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我根据网址" + "https://github.com/kjkyes" +
//                "给该用户生成一个博客网站，不超过30行");
//        Assertions.assertNotNull(result);
//    }
//
//    @Test
//    void generateMultiFileCode() {
//        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("帮我根据网址" + "https://github.com/kjkyes"
//                + "给该用户生成一个留言板，不超过50行");
//        Assertions.assertNotNull(result);
//    }
//
//    @Test
//    void testChatMemory() {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个xk的工具网站，总代码量不超过 20 行");
//        Assertions.assertNotNull(result);
//        result = aiCodeGeneratorService.generateHtmlCode("不要生成网站，告诉我你刚刚做了什么？");
//        Assertions.assertNotNull(result);
//        result = aiCodeGeneratorService.generateHtmlCode("做个xk的工具网站，总代码量不超过 20 行");
//        Assertions.assertNotNull(result);
//        result = aiCodeGeneratorService.generateHtmlCode("不要生成网站，告诉我你刚刚做了什么？");
//        Assertions.assertNotNull(result);
//    }
//
//}