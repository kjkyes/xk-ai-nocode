package com.xk.xkainocode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.xk.xkainocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.xk.xkainocode.model.entity.ChatHistory;
import com.xk.xkainocode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史服务
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话消息
     *
     * @param appId       应用id
     * @param userId      用户id
     * @param message     消息内容
     * @param messageType 消息类型
     * @return
     */
    boolean addChatMessage(Long appId, Long userId, String message, String messageType);

    /**
     * 删除对话消息
     *
     * @param appId 应用id
     * @return
     */
    boolean deleteChatMessage(Long appId);

    /**
     * 获取查询包装类（使用游标查询）
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 分页查询应用对话历史
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 将对话历史加载到内存中
     * @param appId
     * @param chatMemory
     * @param maxCount 最大返回条数
     * @return 加载多少条历史消息到缓存
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
