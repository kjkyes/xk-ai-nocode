package com.xk.xkainocode.ai.agent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.xk.xkainocode.constant.FileConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.io.File;

/**
 * 资源下载工具（根据链接下载文件到本地）
 */
public class ResourceDownloadTool {
    @Tool("Download file from url to local path")
    public String doResourceDownload(@P("The required url") String url,
                                     @P("The path should be saved") String fileName) {
        String fileDir = FileConstant.SAVE_FILE_PATH + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 下载文件
            HttpUtil.downloadFile(url, new File(filePath));
            return "Resource downloaded successfully:" + filePath;
        } catch (Exception e) {
            return "Resource download failed:" + e.getMessage();
        }

    }
}
