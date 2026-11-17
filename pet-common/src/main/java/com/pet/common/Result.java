package com.pet.common;

import lombok.Getter;

/**
 * 业务作用：整个系统的标准接口返回值封装，所有 Controller 方法必须返回此类型。 前端通过 code 判断业务是否成功，通过 data 获取业务数据。
 * 业务规则：
 * - 成功时必须使用 success() 方法构造
 * - 失败时必须使用 error() 方法构造，并给出明确的错误码
 * - 通用错误码：400参数错误、401未认证、403无权限、404不存在、500服务器内部错误
 * 注意事项：此类的 code 是业务错误码，与 HTTP Status Code 不同。 HTTP Status Code 在 GlobalExceptionHandler 中单独映射。
 */
@Getter
public class Result<T> {
    private int code;
    private String message;
    private String errorCode;
    private T data;

    /**
     * 业务作用：构造不含 errorCode 的响应体，仅供静态工厂方法内部使用。
     * 业务规则：构造器私有，禁止外部直接 new。
     * 注意事项：errorCode 保持 null，前端只能依据 code 与 message 判断。
     */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 业务作用：构造带 errorCode 的响应体，供需要稳定错误标识的返回使用。
     * 业务规则：构造器私有，禁止外部直接 new。
     * 注意事项：errorCode 用于前端提示映射，变更会破坏前端兼容。
     */
    private Result(int code, String errorCode, String message, T data) {
        this.code = code;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    /**
     * 返回成功响应，携带业务数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 返回成功响应，无业务数据（仅确认操作成功）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 返回业务错误响应，指定错误码和描述
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 业务作用：返回带业务码、稳定错误标识与提示消息的失败响应。
     * 业务规则：code 取业务码，errorCode 取 BookingErrorCode 等常量。
     * 注意事项：与两参 error 的区别是多了 errorCode，前端应优先读 errorCode。
     */
    public static <T> Result<T> error(int code, String errorCode, String message) {
        return new Result<>(code, errorCode, message, null);
    }

    /**
     * 返回服务器内部错误响应（默认500）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
