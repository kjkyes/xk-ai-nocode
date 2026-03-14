package com.xk.xkainocode.constant;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 点赞/取消点赞的 lua脚本
 */
public class RedisLuaScriptConstant {

    /**
     * 点赞 Lua 脚本
     * KEYS[1]       -- 临时计数键
     * KEYS[2]       -- 用户点赞状态键
     * ARGV[1]       -- 用户 ID
     * ARGV[2]       -- 应用 ID
     * 返回:
     * -1: 已点赞
     * 1: 操作成功
     */
    public static final RedisScript<Long> UPVOTE_SCRIPT = new DefaultRedisScript<>("""  
            local tempUpvoteKey = KEYS[1]       -- 临时计数键（如 upvote:temp:{timeSlice}）  
            local userUpvoteKey = KEYS[2]       -- 用户点赞状态键（如 upvote:{userId}） 
            local userId = ARGV[1]             -- 用户 ID  
            local appId = ARGV[2]              -- 应用 ID  
              
            -- 1. 检查是否已点赞（避免重复操作）  
            if redis.call('HEXISTS', userUpvoteKey, appId) == 1 then  
                return -1  -- 已点赞，返回 -1 表示失败  
            end  
              
            -- 2. 获取旧值（不存在则默认为 0）  
            local hashKey = userId .. ':' .. appId  
            local oldNumber = tonumber(redis.call('HGET', tempUpvoteKey, hashKey) or 0)  
              
            -- 3. 计算新值  
            local newNumber = oldNumber + 1  
              
            -- 4. 原子性更新：写入临时计数 + 标记用户已点赞  
            redis.call('HSET', tempUpvoteKey, hashKey, newNumber)  
            redis.call('HSET', userUpvoteKey, appId, 1)  
              
            return 1  -- 返回 1 表示成功  
            """, Long.class);

    /**
     * 取消点赞 Lua 脚本
     * 参数同上
     * 返回：
     * -1: 未点赞
     * 1: 操作成功
     */
    public static final RedisScript<Long> UNUPVOTE_SCRIPT = new DefaultRedisScript<>("""  
            local tempUpvoteKey = KEYS[1]      -- 临时计数键（如 upvote:temp:{timeSlice}）  
            local userUpvoteKey = KEYS[2]      -- 用户点赞状态键（如 upvote:{userId}）  
            local userId = ARGV[1]            -- 用户 ID  
            local appId = ARGV[2]            -- 应用 ID  
              
            -- 1. 检查用户是否已点赞（若未点赞，直接返回失败）  
            if redis.call('HEXISTS', userUpvoteKey, appId) ~= 1 then  
                return -1  -- 未点赞，返回 -1 表示失败  
            end  
              
            -- 2. 获取当前临时计数（若不存在则默认为 0）  
            local hashKey = userId .. ':' .. appId  
            local oldNumber = tonumber(redis.call('HGET', tempUpvoteKey, hashKey) or 0)  
              
            -- 3. 计算新值并更新  
            local newNumber = oldNumber - 1  
              
            -- 4. 原子性操作：更新临时计数 + 删除用户点赞标记  
            redis.call('HSET', tempUpvoteKey, hashKey, newNumber)  
            redis.call('HDEL', userUpvoteKey, appId)  
              
            return 1  -- 返回 1 表示成功  
            """, Long.class);
}
