package com.xk.xkainocode.job;

import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.constant.AppConstant;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 临时文件清理任务
 * 用于清理截图服务生成的临时图片文件
 */
@Component
@Slf4j
public class TempFileCleanupJob {

    /**
     * 清理临时图片文件
     * @return 清理结果
     */
    @XxlJob("tempFileCleanupJob")
    public String cleanupTempFiles() {
        log.info("开始执行临时截图文件清理任务");
        
        try {
            // 清理截图临时文件
            int screenshotFilesCleaned = cleanupScreenshotTempFiles();
            
            // 清理代码生成临时文件
//            int codeFilesCleaned = cleanupCodeTempFiles();
            
            String result = String.format("临时文件清理完成，共清理截图临时文件 %d 个",
                    screenshotFilesCleaned);
            log.info(result);
            return result;
        } catch (Exception e) {
            log.error("临时文件清理任务执行失败", e);
            return "临时文件清理任务执行失败: " + e.getMessage();
        }
    }

    /**
     * 清理截图临时文件
     * @return 清理的文件数量
     */
    private int cleanupScreenshotTempFiles() {
        AtomicInteger count = new AtomicInteger(0);
        
        // 截图临时文件存储目录（根据实际情况调整）
        String screenshotTempDir = System.getProperty("user.dir") + "/tmp/screenshots";
        
        cleanupTempFiles(screenshotTempDir, count);
        
        return count.get();
    }

    /**
     * 清理代码生成临时文件
     * @return 清理的文件数量
     */
    private int cleanupCodeTempFiles() {
        AtomicInteger count = new AtomicInteger(0);
        
        // 代码生成临时目录
        String codeOutputDir = AppConstant.CODE_OUTPUT_ROOT_DIR;
        String codeDeployDir = AppConstant.CODE_DEPLOY_ROOT_DIR;

        cleanupTempFiles(codeOutputDir, count);
        cleanupTempFiles(codeDeployDir, count);
        
        return count.get();
    }

    /**
     * 清理指定目录下的临时文件
     * @param directoryPath 目录路径
     * @param count 计数器
     */
    private void cleanupTempFiles(String directoryPath, AtomicInteger count) {
        if (StrUtil.isBlank(directoryPath)) {
            return;
        }
        
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            log.debug("目录不存在: {}", directoryPath);
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            log.debug("目录为空: {}", directoryPath);
            return;
        }
        
        long oneDayAgo = LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        for (File file : files) {
            if (file.isFile()) {
                // 清理超过24小时的文件
                if (file.lastModified() < oneDayAgo) {
                    if (file.delete()) {
                        count.incrementAndGet();
                        log.debug("清理文件: {}", file.getAbsolutePath());
                    } else {
                        log.warn("无法删除文件: {}", file.getAbsolutePath());
                    }
                }
            } else if (file.isDirectory()) {
                // 递归清理子目录
                cleanupTempFiles(file.getAbsolutePath(), count);
                
                // 清理空目录
                if (file.listFiles() == null || file.listFiles().length == 0) {
                    if (file.delete()) {
                        log.debug("清理空目录: {}", file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
