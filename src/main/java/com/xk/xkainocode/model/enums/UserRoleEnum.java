package com.xk.xkainocode.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户角色枚举类
 */
@Getter
public enum UserRoleEnum {
    User("用户", "User"),
    Admin("管理员", "Admin");

    // 用户角色名称
    private final String text;

    // 用户角色value
    private final String value;

    // 用于缓存用户角色（适用于用户角色过多的情况）
    private static final Map<String, UserRoleEnum> userRoleMap = new HashMap<>();

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value获取用户角色枚举对象
     *
     * @param value
     * @return 用户角色枚举对象
     */
    public static UserRoleEnum getEnumByValue(String value) {
        // 用户角色不能为空
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        // 从缓存中返回对象
        if (userRoleMap.get(value) != null) {
            return userRoleMap.get(value);
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            userRoleMap.put(userRoleEnum.value, userRoleEnum);
        }
        return null;
    }

}
