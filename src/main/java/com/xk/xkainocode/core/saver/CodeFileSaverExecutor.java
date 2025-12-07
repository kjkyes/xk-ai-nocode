package com.xk.xkainocode.core.saver;

import com.xk.xkainocode.ai.model.HtmlCodeResult;
import com.xk.xkainocode.ai.model.MultiFileCodeResult;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate multiFileSaverTemplate = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行代码保存
     *
     * @param codeResult      代码结果对象
     * @param codeGenTypeEnum 代码类型
     * @param appId 应用 ID
     * @return 保存的目录
     */
    public static File executeSave(Object codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        return switch (codeGenTypeEnum){
            case HTML -> htmlSaverTemplate.saveCodeFile((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileSaverTemplate.saveCodeFile((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码类型");
        };
    }

}
