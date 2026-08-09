package com.pet.common;

import lombok.Getter;

/**
 * 【统一 API 响应体】
 *
 * 业务作用：
 * 整个系统的标准接口返回值封装，所有 Controller 方法必须返回此类型。
 * 前端通过 code 判断业务是否成功，通过 data 获取业务数据。
 *
 * 调用场景：
 * 所有 Controller 方法的统一返回类型，由 GlobalExceptionHandler 兜底异常场景。
 *
 * 调用链：
 * Controller
 *   ↓
 * Result.success() / Result.error()
 *   ↓
 * 前端 JSON 解析
 *
 * 数据处理：
 * 1. code=200 表示业务成功，data 携带业务数据
 * 2. code≠200 表示业务失败，message 描述失败原因
 * 3. 异常场景由 GlobalExceptionHandler 自动包装为 Result.error()
 *
 * 业务规则：
 * - 成功时必须使用 success() 方法构造
 * - 失败时必须使用 error() 方法构造，并给出明确的错误码
 * - 通用错误码：400参数错误、401未认证、403无权限、404不存在、500服务器内部错误
 *
 * 注意事项：
 * 此类的 code 是业务错误码，与 HTTP Status Code 不同。
 * HTTP Status Code 在 GlobalExceptionHandler 中单独映射。
 */
@Getter
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
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
     * 返回服务器内部错误响应（默认500）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
