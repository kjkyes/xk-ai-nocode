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
}
