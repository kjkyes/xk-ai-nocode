package com.xk.xkainocode.ai.model;

import lombok.Data;

/**
 * 结构化输出 - 输出为 json格式
 */
@Data
public class MultiFileCodeResult {

    /**
     * html代码
     */
    private String htmlCode;

    /**
     * css代码
     */
    private String cssCode;

    /**
     * js代码
     */
    private String jsCode;

    /**
     * 描述
     */
    private String description;
}
