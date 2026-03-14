package com.xk.xkainocode.util;

import com.xk.xkainocode.constant.UpvoteConstant;

/**
 * redis key 工具类
 */
public class RedisKeyUtil {

    public static String getUserUpvoteKey(Long userId) {
        return UpvoteConstant.USER_UPVOTE_KEY_PREFIX + userId;
    }

    /**
     * 获取 临时点赞记录 key
     */
    public static String getTempUpvoteKey(String time) {
        return UpvoteConstant.TEMP_UPVOTE_KEY_PREFIX.formatted(time);
    }

    /**
     * 获取应用创建时间 key
     * @param appId
     * @return
     */
    public static String getAppCreateTimeKey(Long appId) {
        return UpvoteConstant.APP_CREATE_TIME_KEY_PREFIX.formatted(appId);
    }
}
