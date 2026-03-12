package com.xk.xkainocode.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.xk.xkainocode.constant.UpvoteConstant;
import com.xk.xkainocode.exception.BusinessException;
import com.xk.xkainocode.exception.ErrorCode;
import com.xk.xkainocode.mapper.UpvoteMapper;
import com.xk.xkainocode.model.dto.upvote.DoUpvoteRequest;
import com.xk.xkainocode.model.entity.App;
import com.xk.xkainocode.model.entity.Upvote;
import com.xk.xkainocode.model.entity.User;
import com.xk.xkainocode.service.UpvoteService;
import com.xk.xkainocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *  服务层实现。
 *
 * @author <a href="https://github.com/kjkyes">xk</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UpvoteServiceImpl extends ServiceImpl<UpvoteMapper, Upvote> implements UpvoteService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 点赞
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @Override
    public Boolean doUpvote(DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        if (doUpvoteRequest == null || doUpvoteRequest.getAppId() == null) {
            throw new RuntimeException("应用ID不能为空");
        }
        Long appId = doUpvoteRequest.getAppId();
        User loginUser = userService.getLoginUser(request);
        return transactionTemplate.execute(status -> {
            // 1. Redis 快速预检
            Boolean isUpvote = hasUpvote(appId, loginUser.getId());
            if (isUpvote) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户已点赞");
            }

            try {
                // 2. 执行插入
                Upvote upvote = new Upvote();
                upvote.setUserId(loginUser.getId());
                upvote.setAppId(appId);
                boolean success = this.save(upvote);

                // 3. 更新Redis
                if (success) {
                    this.changeUpvote(appId, "+");
                    redisTemplate.opsForHash().put(
                            UpvoteConstant.USER_UPVOTE_KEY_PREFIX + loginUser.getId().toString(),
                            appId.toString(), upvote.getId()
                    );
                }
                return success;

            } catch (DuplicateKeyException e) {
                // 4. 数据库兜底
                log.warn("数据库唯一约束拦截了重复点赞: userId={}, appId={}",
                        loginUser.getId(), appId);

                // 尝试修复Redis缓存不一致
                redisTemplate.opsForHash().put(
                        UpvoteConstant.USER_UPVOTE_KEY_PREFIX + loginUser.getId().toString(),
                        appId.toString(), "EXISTS"
                );

                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "您已经点赞过了");
            }
        });
    }

    /**
     * 取消点赞
     * @param doUpvoteRequest
     * @param request
     * @return
     */
    @Override
    public Boolean undoUpvote(DoUpvoteRequest doUpvoteRequest, HttpServletRequest request) {
        if (doUpvoteRequest == null || doUpvoteRequest.getAppId() == null) {
            throw new RuntimeException("应用ID不能为空");
        }
        Long appId = doUpvoteRequest.getAppId();
        User loginUser = userService.getLoginUser(request);
        // 加锁
        synchronized (loginUser.getId().toString().intern()) {

            // 编程式事务
            return transactionTemplate.execute(status -> {

                boolean hasUpvote = redisTemplate.opsForHash().hasKey(UpvoteConstant.USER_UPVOTE_KEY_PREFIX + loginUser.getId().toString(), appId.toString());
                if (!hasUpvote) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户未点赞");
                }
                QueryWrapper queryWrapper = QueryWrapper.create()
                        .eq(Upvote::getUserId, loginUser.getId())
                        .eq(Upvote::getAppId, appId);
                Upvote upvote = this.getOne(queryWrapper);
                // 取消点赞前判断是否已点赞
                if (upvote == null) {
                    // Redis和数据库数据不一致
                    redisTemplate.opsForHash().delete(
                            UpvoteConstant.USER_UPVOTE_KEY_PREFIX + loginUser.getId().toString(),
                            appId.toString()
                    );
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞数据异常");
                }
                // 更新点赞数
                this.changeUpvote(appId, "-");
                boolean success = this.removeById(upvote.getId());
                // 点赞记录从 Redis 删除
                if (success) {
                    redisTemplate.opsForHash().delete(UpvoteConstant.USER_UPVOTE_KEY_PREFIX + loginUser.getId().toString(), appId.toString());
                }
                return success;

            });
        }
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
     * 点赞 +1/-1
     * @param appId
     */
    private void changeUpvote(Long appId, String symbol) {
        UpdateChain.of(App.class)
                .set(App::getId, appId)
                .setRaw(App::getUpvoteCount, "upvoteCount" + symbol + "1")
                .where(App::getId).eq(appId)
                .update();
    }

}