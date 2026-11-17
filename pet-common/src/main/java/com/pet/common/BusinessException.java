package com.pet.common;

/**
 * 业务作用：系统核心异常类，用于在 Service 层抛出可预期的业务错误。 由 GlobalExceptionHandler 统一捕获并转换为标准 Result 错误响应。
 * 注意事项：- 此异常不自带日志打印，由 GlobalExceptionHandler 统一记录 - 与 Spring Security 的 AccessDeniedException 分开处理
 */
public class BusinessException extends RuntimeException {
    private int code;
    private String errorCode;

    /**
     * 业务作用：构造只带业务码与提示消息的业务异常。
     * 业务规则：code 为业务码，与 HTTP 状态码的映射由 GlobalExceptionHandler 完成。
     * 异常情况：无；构造本身不校验 code 合法性。
     * 注意事项：未设置 errorCode，前端只能依据 code 与 message 判断。
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 业务作用：构造同时带业务码、稳定错误标识与提示消息的异常。
     * 业务规则：errorCode 取值应来自 BookingErrorCode 等稳定标识常量。
     * 注意事项：errorCode 一经发布不应变更，前端依赖它做提示映射。
     */
    public BusinessException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    /**
     * 业务作用：构造只带提示消息的业务异常，业务码固定为 500。
     * 业务规则：一律按服务器内部错误处理。
     * 注意事项：会把业务错误误报为 500，新代码应显式传入业务码。
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() { return code; }

    public String getErrorCode() { return errorCode; }
}
