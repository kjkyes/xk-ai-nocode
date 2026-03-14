package com.xk.xkainocode.common;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.xk.xkainocode.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 接口响应封装类
 *
 * @param <T>
 */
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,  // 使用类的全限定名作为类型标识
        include = JsonTypeInfo.As.PROPERTY,  // 作为属性包含在JSON中
        property = "@class"  // 属性名为 @class
)
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
