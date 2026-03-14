package com.xk.xkainocode.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞枚举
 */
@Getter
public enum UpvoteEnum {
    // 点赞
    Incr("+1", 1),
    // 取消点赞
    Decr("-1", -1),
    // 未变动
    Non("0", 0);

    // 用户角色名称
    private final String text;

    // 用户角色value
    private final Integer value;

    // 用于缓存点赞记录（适用于点赞记录过多的情况）
    private static final Map<Integer, UpvoteEnum> upvoteEnumMap = new HashMap<>();

    UpvoteEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value获取点赞枚举对象
     *
     * @param value
     * @return 点赞枚举对象
     */
    public static UpvoteEnum getEnumByValue(Integer value) {
        // 点赞不能为空
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        // 从缓存中返回对象
        if (upvoteEnumMap.get(value) != null) {
            return upvoteEnumMap.get(value);
        }
        for (UpvoteEnum upvoteEnum : UpvoteEnum.values()) {
            upvoteEnumMap.put(upvoteEnum.value, upvoteEnum);
        }
        return null;
    }

}
