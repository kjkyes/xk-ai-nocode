package com.xk.xkainocode.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.mybatisflex.core.query.QueryWrapper;
import com.xk.xkainocode.mapper.AppMapper;
import com.xk.xkainocode.model.entity.Upvote;
import com.xk.xkainocode.model.enums.UpvoteEnum;
import com.xk.xkainocode.service.UpvoteService;
import com.xk.xkainocode.util.RedisKeyUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 定时将 Redis 中的临时点赞数据同步到数据库
 *
 */
@Component
@Slf4j
public class SyncUpvote2DBJob {

    @Resource
    private UpvoteService upvoteService;

    @Resource
    private AppMapper appMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedRate = 10000)
    @Transactional(rollbackFor = Exception.class)
    public void run() {
        log.info("开始执行数据同步");
        DateTime nowDate = DateUtil.date();
        // 如果秒数为0~9 则回到上一分钟的50秒
        int second = (DateUtil.second(nowDate) / 10 - 1) * 10;
        if (second == -10) {
            second = 50;
            // 回到上一分钟
            nowDate = DateUtil.offsetMinute(nowDate, -1);
        }
        String date = DateUtil.format(nowDate, "HH:mm:") + second;
        syncUpvote2DBByDate(date);
        log.info("临时数据同步完成");
    }

    public void syncUpvote2DBByDate(String date) {
        // 获取到临时点赞和取消点赞数据
        String tempUpvoteKey = RedisKeyUtil.getTempUpvoteKey(date);
        Map<Object, Object> allTempUpvoteMap = redisTemplate.opsForHash().entries(tempUpvoteKey);
        boolean upvoteMapEmpty = CollUtil.isEmpty(allTempUpvoteMap);

        // 同步 点赞 到数据库
        // 构建插入列表并收集appId
        Map<Long, Long> appUpvoteCountMap = new HashMap<>();
        if (upvoteMapEmpty) {
            return;
        }
        ArrayList<Upvote> upvoteList = new ArrayList<>();
        boolean needRemove = false;
        boolean isFirst = true;
        QueryWrapper wrapper = QueryWrapper.create();
        // 遍历临时点赞数据
        for (Object userIdAppIdObj : allTempUpvoteMap.keySet()) {
            String userIdAppId = (String) userIdAppIdObj;
            String[] userIdAndAppId = userIdAppId.split(StrPool.COLON);
            Long userId = Long.valueOf(userIdAndAppId[0]);
            Long appId = Long.valueOf(userIdAndAppId[1]);
            // -1 取消点赞，1 点赞
            Integer upvoteType = Integer.valueOf(allTempUpvoteMap.get(userIdAppId).toString());
            if (upvoteType.equals(UpvoteEnum.Incr.getValue())) {
                Upvote upvote = new Upvote();
                upvote.setUserId(userId);
                upvote.setAppId(appId);
                upvoteList.add(upvote);
            } else if (upvoteType.equals(UpvoteEnum.Decr.getValue())) {
                // 拼接查询条件，批量删除
                needRemove = true;
                if(isFirst){
                    wrapper.where(Upvote::getUserId).eq(userId)
                            .and(Upvote::getAppId).eq(appId);
                    isFirst = false;
                }else {
                    wrapper.or(Upvote::getUserId).eq(userId)
                    .and(Upvote::getAppId).eq(appId);
                }
            } else {
                if (!upvoteType.equals(UpvoteEnum.Non.getValue())) {
                    log.warn("无效操作：{}", userId + "," + appId + "," + upvoteType);
                }
                continue;
            }
            // 计算点赞增量
            appUpvoteCountMap.put(appId, appUpvoteCountMap.getOrDefault(appId, 0L) + upvoteType);
        }
        // 批量插入
        upvoteService.saveBatch(upvoteList);
        // 批量删除
        if (needRemove) {
            upvoteService.remove(wrapper);
        }
        // 批量更新应用点赞量
        if (!appUpvoteCountMap.isEmpty()) {
            appMapper.batchUpdateUpvoteCount(appUpvoteCountMap);
        }
        // 异步删除
        Thread.startVirtualThread(() -> {
            redisTemplate.delete(tempUpvoteKey);
        });
    }
}

