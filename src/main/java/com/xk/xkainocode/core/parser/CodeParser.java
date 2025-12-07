package com.xk.xkainocode.core.parser;

/**
 * 代码解析器策略接口（策略模式）
 */
public interface CodeParser<T> {

    /**
     * 解析代码
     * @param codeContent
     * @return
     */
    T parseCode(String codeContent);
}
