package com.xk.xkainocode.model.dto.upvote;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞请求
 */
@Data
public class DoUpvoteRequest implements Serializable {
    /**
     * 应用id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;

}
