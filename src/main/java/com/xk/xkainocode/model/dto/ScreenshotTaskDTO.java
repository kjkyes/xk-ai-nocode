package com.xk.xkainocode.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 截图任务DTO，用于消息队列传递
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScreenshotTaskDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 要截图的网页URL
     */
    private String webUrl;
    
    /**
     * 应用ID（可选，用于更新应用封面）
     */
    private Long appId;
}