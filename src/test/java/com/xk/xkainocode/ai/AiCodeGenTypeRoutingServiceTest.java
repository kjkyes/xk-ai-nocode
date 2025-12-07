//package com.xk.xkainocode.ai;
//
//import com.xk.xkainocode.model.enums.CodeGenTypeEnum;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//@Slf4j
//class AiCodeGenTypeRoutingServiceTest {
//
//    @Resource
//    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
//
//    @Test
//    void routeCodeGenType() {
//        String userPrompt = "做一个简单的个人介绍页面";
//        String raw = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt); // 先当 String 接
//        String clean = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
//        CodeGenTypeEnum type = CodeGenTypeEnum.valueOf(clean);
//        log.info("用户需求：{} -> {}", userPrompt, type);
//
//        userPrompt = "做一个公司官网，需要首页、联系我们、关于我们三个页面";
//        raw = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt); // 先当 String 接
//        clean = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
//        type = CodeGenTypeEnum.valueOf(clean);
//        log.info("用户需求：{} -> {}", userPrompt, type);
//
//        userPrompt = "做一个电商管理系统，包含用户管理、商品管理、订单管理，需要路由和状态管理";
//        raw = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt); // 先当 String 接
//        clean = raw.replaceAll("(?s)<think>.*?</think>", "").trim();
//        type = CodeGenTypeEnum.valueOf(clean);
//        log.info("用户需求: {} -> {}", userPrompt, type);
//
//    }
//}