package com.xk.xkainocode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xk.xkainocode.constant.UserConstant;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.exception.ThrowUtils;
import com.xk.xkainocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.xk.xkainocode.model.entity.App;
import com.xk.xkainocode.model.entity.ChatHistory;
import com.xk.xkainocode.mapper.ChatHistoryMapper;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.enums.ChatHistoryMessageTypeEnum;
import com.xk.xkainocode.service.AppService;
import com.xk.xkainocode.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;

    /**
     * 添加对话历史
     * @param appId       应用id
     * @param userId      用户id
     * @param message     消息内容
     * @param messageType 消息类型
     * @return
     */
    @Override
    public boolean addChatMessage(Long appId, Long userId, String message, String messageType) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"appId不能为空");
        ThrowUtils.throwIf(userId == null || userId < 0, ErrorCode.PARAMS_ERROR,"userId不能为空");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR,"message不能为空");
        ThrowUtils.throwIf(messageType == null || messageType.isEmpty(), ErrorCode.PARAMS_ERROR,"messageType不能为空");
        // 将数据保存到数据库中
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .build();
        return this.save(chatHistory);
    }

    /**
     * 删除某 app 的对话历史
     * @param appId 应用id
     * @return
     */
    @Override
    public boolean deleteChatMessage(Long appId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"appId不能为空");
        // 删除数据
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param loginUser      当前登录用户
     * @return 对话历史分页
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 加载应用的历史对话到内存中
     * @param appId
     * @param chatMemory
     * @param maxCount 最大返回条数
     * @return
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 构造QueryWrapper
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            // 如果数据库中没有历史消息直接返回0条
            if(CollUtil.isEmpty(historyList)){
                return 0;
            }
            // 将对话历史反转，按时间从早到晚
            historyList = historyList.reversed();
            // 按时间顺序将历史消息添加到记忆中
            int loadedCount = 0;
            // 将缓存中的历史消息清空
            chatMemory.clear();
            for (ChatHistory chatHistory : historyList) {
                if(ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }else if(ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }
            }
            log.info("加载应用id：{} 共 {}条历史消息", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载应用id：{} 历史消息失败", appId, e);
            return 0;
        }
    }

}
