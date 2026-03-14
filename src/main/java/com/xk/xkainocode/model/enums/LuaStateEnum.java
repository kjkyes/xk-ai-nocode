package com.xk.xkainocode.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * lua 脚本状态枚举
 */
@Getter
public enum LuaStateEnum {
    Success("success", 1),
    Fail("fail", -1);

    // 用户角色名称
    private final String text;

    // 用户角色value
    private final Integer value;

    // 用于缓存点赞记录（适用于点赞记录过多的情况）
    private static final Map<Integer, LuaStateEnum> luaStateEnumMap = new HashMap<>();

    LuaStateEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value获取 Lua执行结果枚举对象
     *
     * @param value
     * @return lua执行结果枚举对象
     */
    public static LuaStateEnum getEnumByValue(Integer value) {
        // 点赞不能为空
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        // 从缓存中返回对象
        if (luaStateEnumMap.get(value) != null) {
            return luaStateEnumMap.get(value);
        }
        for (LuaStateEnum luaStateEnum : LuaStateEnum.values()) {
            luaStateEnumMap.put(luaStateEnum.value, luaStateEnum);
        }
        return null;
    }
}
