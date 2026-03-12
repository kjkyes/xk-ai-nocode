package com.xk.xkainocode.service;

import com.mybatisflex.core.service.IService;
import com.xk.xkainocode.model.dto.upvote.DoUpvoteRequest;
import com.xk.xkainocode.model.entity.Upvote;
import jakarta.servlet.http.HttpServletRequest;

/**
 *  服务层。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
public interface UpvoteService extends IService<Upvote> {

    /**
     * 点赞
     * @param doUpvoteRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doUpvote(DoUpvoteRequest doUpvoteRequest, HttpServletRequest request);

    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoUpvote(DoUpvoteRequest doThumbRequest, HttpServletRequest request);

    /**
     * 查询点赞状态
     * @param appId
     * @param userId
     * @return {@link Boolean }
     */
    Boolean hasUpvote(Long appId, Long userId);

}
