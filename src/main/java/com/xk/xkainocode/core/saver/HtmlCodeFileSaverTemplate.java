package com.xk.xkainocode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.xk.xkainocode.ai.model.HtmlCodeResult;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.model.enums.CodeGenTypeEnum;

/**
 * 保存HTML文件模板类
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String fileDir) {
        writeToFile(fileDir, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validInput(HtmlCodeResult result, Long appId){
        super.validInput(result, appId);
        // html 代码不能为空
        if(StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码不能为空");
        }
    }
}
