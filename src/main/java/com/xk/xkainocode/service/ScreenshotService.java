package com.xk.xkainocode.service;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 通用的截图服务，可以得到访问地址
     *
     * @param url
     * @return
     */
    String generateAndUploadScreenshot(String url);
    
    /**
     * 生成网页截图并上传到对象存储，完成后更新应用封面
     *
     * @param url   网页URL
     * @param appId 应用ID
     * @return 任务处理结果
     */
    String generateAndUploadScreenshot(String url, Long appId);
}
