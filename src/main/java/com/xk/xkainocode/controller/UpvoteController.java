package com.xk.xkainocode.controller;

import com.xk.xkainocode.common.BaseResponse;
import com.xk.xkainocode.common.ResultUtils;
import com.xk.xkainocode.model.dto.upvote.DoUpvoteRequest;
import com.xk.xkainocode.service.UpvoteService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  点赞
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@RestController
@RequestMapping("/upvote")
public class UpvoteController {

    @Resource
    private UpvoteService upvoteService;

    /**
     * 点赞
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @PostMapping("/doUpvote")
    public BaseResponse<Boolean> doUpvote(@RequestBody DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        Boolean success = upvoteService.doUpvote(doUpvoteRequest, request);
        return ResultUtils.success(success);
    }

    /**
     * 取消点赞
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @PostMapping("/undoUpvote")
    public BaseResponse<Boolean> undoThumb(@RequestBody DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        Boolean success = upvoteService.undoUpvote(doUpvoteRequest, request);
        return ResultUtils.success(success);
    }

}
