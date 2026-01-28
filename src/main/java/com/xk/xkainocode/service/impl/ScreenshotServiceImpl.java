package com.xk.xkainocode.service.impl;

import com.xk.xkainocode.config.RabbitMQConfig;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.exception.ThrowUtils;
import com.xk.xkainocode.model.dto.ScreenshotTaskDTO;
import com.xk.xkainocode.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        return generateAndUploadScreenshot(webUrl, null);
    }
    
    @Override
    public String generateAndUploadScreenshot(String webUrl, Long appId) {
        // 参数校验
        ThrowUtils.throwIf(webUrl == null || webUrl.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        
        // 生成任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 创建截图任务
        ScreenshotTaskDTO task = ScreenshotTaskDTO.builder()
                .taskId(taskId)
                .webUrl(webUrl)
                .appId(appId)
                .build();
        
        // 发送到消息队列
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCREENSHOT_TASK_EXCHANGE,
                RabbitMQConfig.SCREENSHOT_TASK_ROUTING_KEY,
                task
        );
        
        log.info("截图任务已发送到队列: taskId={}, webUrl={}, appId={}", taskId, webUrl, appId);
        return "截图任务已提交，正在处理中...";
    }
}