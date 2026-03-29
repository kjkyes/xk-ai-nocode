package com.xk.xkainocode.ai.agent.model;

/**
 * agent 执行状态的枚举类
 */
public enum AgentState {

    /**
     * 空闲状态
     */
    IDLE,
    /**
     * 执行中状态
     */
    RUNNING,
    /**
     * 完成状态
     */
    FINISHED,
    /**
     * 错误状态
     */
    ERROR
}
