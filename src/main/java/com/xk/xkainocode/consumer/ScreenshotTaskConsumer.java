package com.xk.xkainocode.consumer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.config.RabbitMQConfig;
import com.xk.xkainocode.manager.CosManager;
import com.xk.xkainocode.model.dto.ScreenshotTaskDTO;
import com.xk.xkainocode.model.entity.App;
import com.xk.xkainocode.service.AppService;
import com.xk.xkainocode.util.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 截图任务消费者
 * 从RabbitMQ队列中取出截图任务并执行
 */
@Component
@Slf4j
public class ScreenshotTaskConsumer {
    
    @Resource
    private CosManager cosManager;
    
    @Resource
    private AppService appService;
    
    /**
     * 监听截图任务队列
     * 配置concurrency="3-10"表示线程池大小在3-10之间动态调整
     */
    @RabbitListener(queues = RabbitMQConfig.SCREENSHOT_TASK_QUEUE, concurrency = "3-10")
    public void handleScreenshotTask(ScreenshotTaskDTO task) {
        log.info("开始处理截图任务: taskId={}, webUrl={}, appId={}", 
                task.getTaskId(), task.getWebUrl(), task.getAppId());
        
        try {
            // 1. 执行截图
            String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(task.getWebUrl());
            if (StrUtil.isBlank(localScreenshotPath)) {
                log.error("截图生成失败: taskId={}, webUrl={}", task.getTaskId(), task.getWebUrl());
                return;
            }
            
            try {
                // 2. 上传到对象存储
                String cosUrl = uploadScreenshotToCos(localScreenshotPath);
                if (StrUtil.isBlank(cosUrl)) {
                    log.error("截图上传失败: taskId={}, webUrl={}", task.getTaskId(), task.getWebUrl());
                    return;
                }
                
                log.info("截图任务完成: taskId={}, webUrl={}, cosUrl={}", 
                        task.getTaskId(), task.getWebUrl(), cosUrl);
                
                // 3. 如果有appId，更新应用封面
                if (task.getAppId() != null) {
                    updateAppCover(task.getAppId(), cosUrl);
                }
            } finally {
                // 4. 清理本地文件
                cleanupLocalFile(localScreenshotPath);
            }
        } catch (Exception e) {
            log.error("处理截图任务异常: taskId={}, webUrl={}", task.getTaskId(), task.getWebUrl(), e);
        }
    }
    
    /**
     * 上传截图到对象存储
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }
    
    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }
    
    /**
     * 清理本地文件
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            java.io.File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("本地截图文件已清理: {}", localFilePath);
        }
    }
    
    /**
     * 更新应用封面
     */
    private void updateAppCover(Long appId, String screenshotUrl) {
        try {
            // 调用AppService更新应用封面
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = appService.updateById(updateApp);
            if (updated) {
                log.info("应用封面更新成功: appId={}, coverUrl={}", appId, screenshotUrl);
            } else {
                log.error("应用封面更新失败: appId={}", appId);
            }
        } catch (Exception e) {
            log.error("更新应用封面异常: appId={}", appId, e);
        }
    }
}