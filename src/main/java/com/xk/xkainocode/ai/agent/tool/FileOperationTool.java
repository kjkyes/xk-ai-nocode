package com.xk.xkainocode.ai.agent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.xk.xkainocode.constant.FileConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;


/**
 * 文件操作工具类（文件读写功能）
 */
public class FileOperationTool {
    public final String saveFilePath = FileConstant.SAVE_FILE_PATH + "/file";

    @Tool("Read content from a file")
    public String readFile(@P("Name of a file") String fileName) {
        String file_Path = saveFilePath + "/" + fileName;
        try {
            return FileUtil.readUtf8String(file_Path);
        } catch (IORuntimeException e) {
            return "Error: read file failed:" + e.getMessage();
        }
    }

    @Tool("write content to a file")
    public String writeFile(@P("name of a file") String fileName,
                            @P("the required content of a file") String content) {
        String file_Path = saveFilePath + "/" + fileName;
        try {
            FileUtil.mkdir(saveFilePath);
            FileUtil.writeUtf8String(content, file_Path);
            return "Success: write file successfully";
        } catch (IORuntimeException e) {
            return "Error: write file failed:" + e.getMessage();
        }
    }
}
