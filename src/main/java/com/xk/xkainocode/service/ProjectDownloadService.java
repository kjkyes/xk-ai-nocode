package com.xk.xkainocode.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 代码文件下载服务
 */
public interface ProjectDownloadService {

    /**
     * 压缩可下载文件为 zip文件
     * @param projectPath
     * @param downloadFileName
     * @param response
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
