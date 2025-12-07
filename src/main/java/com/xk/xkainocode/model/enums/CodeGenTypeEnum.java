package com.xk.xkainocode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {
    HTML("原生HTML模式", "html"),
    MULTI_FILE("多文件模式", "multiFile"),
    VUE_PROJECT("Vue项目模式", "vueProject");

    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value获取枚举对象
     * @param value
     * @return
     */
    public static CodeGenTypeEnum getEnumByValue(String value){
        if(ObjUtil.isNull(value)){
            return null;
        }
        for(CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()){
            if(anEnum.value.equals(value)){
                return anEnum;
            }
        }
        return null;
    }
}
