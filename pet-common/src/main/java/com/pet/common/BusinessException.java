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

    /**
     * 【业务异常构造器（业务码 + 消息）】
     *
     * 业务作用：构造只带业务码与提示消息的业务异常。
     *
     * 调用场景：Service 层抛出参数错误、权限不足、数据不存在等可预期业务错误。
     *
     * 调用链：Service.xxx → new BusinessException(code, message) → GlobalExceptionHandler → Result.error。
     *
     * 数据处理：code 写入业务码字段，message 交给父类 RuntimeException 保存。
     *
     * 业务规则：code 为业务码，与 HTTP 状态码的映射由 GlobalExceptionHandler 完成。
     *
     * 状态影响：无。
     *
     * 异常情况：无；构造本身不校验 code 合法性。
     *
     * 注意事项：未设置 errorCode，前端只能依据 code 与 message 判断。
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 【业务异常构造器（业务码 + 稳定标识 + 消息）】
     *
     * 业务作用：构造同时带业务码、稳定错误标识与提示消息的异常。
     *
     * 调用场景：预约、产品图册等需要前端按 errorCode 精确映射提示的流程。
     *
     * 调用链：Service.xxx → new BusinessException(code, errorCode, message) → GlobalExceptionHandler。
     *
     * 数据处理：code、errorCode、message 分别写入对应字段。
     *
     * 业务规则：errorCode 取值应来自 BookingErrorCode 等稳定标识常量。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：errorCode 一经发布不应变更，前端依赖它做提示映射。
     */
    public BusinessException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    /**
     * 【业务异常构造器（仅消息）】
     *
     * 业务作用：构造只带提示消息的业务异常，业务码固定为 500。
     *
     * 调用场景：无法区分错误类型的兜底场景。
     *
     * 调用链：Service.xxx → new BusinessException(message) → GlobalExceptionHandler。
     *
     * 数据处理：message 交给父类，code 固定 500。
     *
     * 业务规则：一律按服务器内部错误处理。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：会把业务错误误报为 500，新代码应显式传入业务码。
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() { return code; }

    public String getErrorCode() { return errorCode; }
}
