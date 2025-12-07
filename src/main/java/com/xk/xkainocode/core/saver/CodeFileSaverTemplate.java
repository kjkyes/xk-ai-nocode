package com.xk.xkainocode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.constant.AppConstant;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件保存器模板（模板模式）
 */
public abstract class CodeFileSaverTemplate<T> {

    /**
     * 文件保存的根目录
     */
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;


    /**
     * 模板方法，保存代码的标准流程
     * 流程不可更改
     *
     * @param result 代码结果对象
     * @param appId 应用 ID
     * @return 保存的目录
     */
    public final File saveCodeFile(T result, Long appId){
        // 1.验证输入
        validInput(result, appId);
        // 2.构建唯一目录
        String fileDir = buildUniqueDir(appId);
        // 3.保存文件
        saveFiles(result, fileDir);
        // 4.返回目录文件对象
        return new File(fileDir);
    }

    /**
     * 验证输入
     *
     * @param result 代码结果对象
     * @param appId 应用 ID
     */
    protected void validInput(T result, Long appId) {
        if(result == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "输入不能为空");
        }
        if(appId == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用 ID 不能为空");
        }
    }

    /**
     * 构建文件的唯一路径：tmp/code_output/bizType_雪花id
     * 雪花 id 旨在将一个随机数分成64位，分别表示机器码、时间戳等，因为时间戳的因素，所以能保证生成的id唯一
     * 但也因此要注意雪花 id在服务器时间会退时可能重复
     *
     * @return 保存文件目录
     */
    protected String buildUniqueDir(Long appId) {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String uniqueDirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(uniqueDirPath);
        return uniqueDirPath;
    }

    /**
     * 保存单个文件
     * 写文件逻辑不可更改
     *
     * @param dirPath
     * @param fileName
     * @param content
     */
    public final void writeToFile(String dirPath, String fileName, String content) {
        if(StrUtil.isNotBlank(content)){
            String filePath = dirPath + File.separator + fileName;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    protected abstract CodeGenTypeEnum getCodeType();

    protected abstract void saveFiles(T result, String fileDir);
}
