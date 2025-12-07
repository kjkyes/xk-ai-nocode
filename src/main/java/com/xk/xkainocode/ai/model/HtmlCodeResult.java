package com.xk.xkainocode.ai.model;

import lombok.Data;

/**
 * 结构化输出 - 输出为 json格式
 */
@Data
public class HtmlCodeResult {

    /**
     * html代码
     */
    private String htmlCode;

    /**
     * 描述
     */
    private String description;
}
