package com.xk.xkainocode.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xk.xkainocode.constant.RedisLuaScriptConstant;
import com.xk.xkainocode.constant.RedisLuaScriptOnlyCountConstant;
import com.xk.xkainocode.constant.UpvoteConstant;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.mapper.UpvoteMapper;
import com.xk.xkainocode.model.dto.upvote.DoUpvoteRequest;
import com.xk.xkainocode.model.entity.Upvote;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.model.enums.LuaStateEnum;
import com.xk.xkainocode.service.UpvoteService;
import com.xk.xkainocode.service.UserService;
import com.xk.xkainocode.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 *  服务层实现。
 *  相较于upvoteServiceDB，使用Lua脚本代替原本的事务+锁的实现方式
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@Service("upvoteService")
@Slf4j
@RequiredArgsConstructor
public class UpvoteServiceRedisImpl extends ServiceImpl<UpvoteMapper, Upvote> implements UpvoteService {

    private final UserService userService;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 点赞
     *
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @Override
    public Boolean doUpvote(DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        if (doUpvoteRequest == null || doUpvoteRequest.getAppId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        User loginUser = userService.getLoginUser(request);
        Long appId = doUpvoteRequest.getAppId();
        String timeSlice = getTimeSlice();
        // Redis Key
        String tempUpvoteKey = RedisKeyUtil.getTempUpvoteKey(timeSlice);
        String userUpvoteKey = RedisKeyUtil.getUserUpvoteKey(loginUser.getId());
        String appCreateTimeKey = RedisKeyUtil.getAppCreateTimeKey(appId);
        // 判断点赞记录是否过期
        if (isExpired(appCreateTimeKey, userUpvoteKey)) {
            // 查询点赞状态
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq(Upvote::getUserId, loginUser.getId())
                    .eq(Upvote::getAppId, appId);
            boolean exists = this.exists(wrapper);
            if (!exists) {
                // 执行 Lua 脚本
                redisTemplate.execute(
                        RedisLuaScriptOnlyCountConstant.UPVOTE_ONLY_COUNT_SCRIPT,
                        Collections.singletonList(tempUpvoteKey),
                        loginUser.getId(),
                        appId
                );
                return true;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户已点赞");
        }
        // 未过期查Redis并执行点赞
        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.UPVOTE_SCRIPT,
                Arrays.asList(tempUpvoteKey, userUpvoteKey),
                loginUser.getId(),
                appId
        );

        if (LuaStateEnum.Fail.getValue() == result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户已点赞");
        }
        // 更新成功才执行
        return LuaStateEnum.Success.getValue() == result;
    }

    /**
     * 取消点赞
     *
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @Override
    public Boolean undoUpvote(DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        if (doUpvoteRequest == null || doUpvoteRequest.getAppId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        User loginUser = userService.getLoginUser(request);

        Long appId = doUpvoteRequest.getAppId();
        // 计算时间片
        String timeSlice = getTimeSlice();
        // Redis Key
        String tempUpvoteKey = RedisKeyUtil.getTempUpvoteKey(timeSlice);
        String userUpvoteKey = RedisKeyUtil.getUserUpvoteKey(loginUser.getId());
        String appCreateTimeKey = RedisKeyUtil.getAppCreateTimeKey(appId);
        // 判断点赞记录是否过期
        if (isExpired(appCreateTimeKey, userUpvoteKey)) {
            // 查询点赞状态
            QueryWrapper wrapper = QueryWrapper.create()
                    .eq(Upvote::getUserId, loginUser.getId())
                    .eq(Upvote::getAppId, appId);
            boolean exists = this.exists(wrapper);
            if (!exists) {
                // 执行 Lua 脚本
                redisTemplate.execute(
                        RedisLuaScriptOnlyCountConstant.UNUPVOTE_ONLY_COUNT_SCRIPT,
                        Collections.singletonList(tempUpvoteKey),
                        loginUser.getId(),
                        appId
                );
                return true;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户已点赞");
        }
        // 执行 Lua 脚本
        long result = redisTemplate.execute(
                RedisLuaScriptConstant.UNUPVOTE_SCRIPT,
                Arrays.asList(tempUpvoteKey, userUpvoteKey),
                loginUser.getId(),
                appId
        );
        // 根据返回值处理结果
        if (LuaStateEnum.Fail.getValue() == result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户未点赞");
        }

        // 更新成功才执行
        return LuaStateEnum.Success.getValue() == result;
    }

    private String getTimeSlice() {
        DateTime nowDate = DateUtil.date();
        // 获取到当前时间前最近的整数秒，比如当前 11:20:23 ，获取到 11:20:20
        return DateUtil.format(nowDate, "HH:mm:") + (DateUtil.second(nowDate) / 10) * 10;
    }

    /**
     * 查看点赞状态
     * @param appId
     * @param userId
     * @return
     */
    @Override
    public Boolean hasUpvote(Long appId, Long userId) {
        return redisTemplate.opsForHash().hasKey(UpvoteConstant.USER_UPVOTE_KEY_PREFIX + userId.toString(), appId.toString());
    }

    /**
     * 判断用户点赞记录是否该过期
     * @param appCreateTimeKey
     * @param userUpvoteKey
     * @return
     */
    private boolean isExpired(String appCreateTimeKey, String userUpvoteKey) {
        String appCreateTime = (String) redisTemplate.opsForValue().get(appCreateTimeKey);
        if(appCreateTime == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用不存在");
        }
        int old_year = Integer.parseInt(appCreateTime.substring(0, 4));
        int old_month = Integer.parseInt((appCreateTime.substring(5)));
        DateTime nowDate = DateUtil.date();
        int year = DateUtil.year(nowDate);
        int month = DateUtil.month(nowDate) + 1;
        // 判断点赞记录是否过期
        if (year > old_year || (year == old_year && month >= old_month + 1)) {
            // 已过期，删除redis中的用户点赞记录
            deleteUpvoteRecord(userUpvoteKey);
            return true;
        }
        return false;
    }

    /**
     * 异步删除 redis点赞状态记录
     * @param tempUpvoteKey
     */
    private void deleteUpvoteRecord(String tempUpvoteKey) {
        // 异步删除
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempUpvoteKey);
        });
    }
}