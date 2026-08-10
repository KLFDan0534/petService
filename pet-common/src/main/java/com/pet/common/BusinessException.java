package com.pet.common;

/**
 * 【业务异常】
 *
 * 业务作用：
 * 系统核心异常类，用于在 Service 层抛出可预期的业务错误。
 * 由 GlobalExceptionHandler 统一捕获并转换为标准 Result 错误响应。
 *
 * 调用场景：
 * Service/Impl 中校验失败、数据不存在、权限不足等业务逻辑错误时抛出。
 *
 * 调用链：
 * Service.xxx()
 *   ↓
 * throw new BusinessException(code, message)
 *   ↓
 * GlobalExceptionHandler.handleBusinessException()
 *   ↓
 * Result.error() → 前端
 *
 * 数据处理：
 * code 为业务错误码，HTTP Status 在 GlobalExceptionHandler 中根据 code 映射：
 * - 400 → BAD_REQUEST
 * - 401 → UNAUTHORIZED
 * - 403 → FORBIDDEN
 * - 404 → NOT_FOUND
 * - 其他 → INTERNAL_SERVER_ERROR
 *
 * 注意事项：
 * - 此异常不自带日志打印，由 GlobalExceptionHandler 统一记录
 * - 与 Spring Security 的 AccessDeniedException 分开处理
 */
public class BusinessException extends RuntimeException {
    private int code;
    private String errorCode;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() { return code; }

    public String getErrorCode() { return errorCode; }
}
