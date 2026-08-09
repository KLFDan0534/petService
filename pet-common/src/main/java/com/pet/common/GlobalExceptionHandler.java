package com.pet.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

/**
 * 【全局异常处理器】
 *
 * 业务作用：
 * 统一拦截所有 Controller 层抛出的异常，转换为标准 Result 响应格式返回前端。
 * 避免异常信息泄露，提供用户友好的错误提示。
 *
 * 调用场景：
 * 所有 Controller 方法执行过程中抛出异常时自动触发。
 *
 * 调用链：
 * Controller
 *   ↓
 * Service → BusinessException
 *   ↓
 * GlobalExceptionHandler
 *   ↓
 * Result.error() → 前端
 *
 * 异常映射规则：
 * - BusinessException(code, msg)：根据 code 映射 HTTP Status
 * - AccessDeniedException：403 无权限
 * - MethodArgumentNotValidException：400 参数校验失败（@Valid）
 * - NoResourceFoundException：404 资源不存在
 * - MissingServletRequestParameterException：400 缺少必填参数
 * - HttpRequestMethodNotSupportedException：405 不支持的请求方法
 * - HttpMessageNotReadableException：400 请求体格式错误
 * - HttpMediaTypeNotSupportedException：415 不支持的 Content-Type
 * - DateTimeParseException：400 日期格式错误
 * - Exception（兜底）：500 服务器内部错误，打印完整堆栈
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常，根据业务错误码映射 HTTP 状态码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        int httpStatus = switch (e.getCode()) {
            case 400 -> HttpStatus.BAD_REQUEST.value();
            case 401 -> HttpStatus.UNAUTHORIZED.value();
            case 403 -> HttpStatus.FORBIDDEN.value();
            case 404 -> HttpStatus.NOT_FOUND.value();
            default -> HttpStatus.INTERNAL_SERVER_ERROR.value();
        };
        return ResponseEntity.status(httpStatus).body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理 Spring Security 权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(403, "访问被拒绝");
    }

    /**
     * 处理 @Valid 参数校验失败异常，收集所有校验失败信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, msg);
    }

    /**
     * 处理 404 资源不存在异常（URL 路径错误）
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFoundException(Exception e) {
        log.warn("Request path not found: {}", e.getMessage());
        return Result.error(404, "资源未找到");
    }

    /**
     * 处理缺少必填请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 处理请求方法不支持异常（如 GET 调 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "不支持的请求方法");
    }

    /**
     * 处理请求体为空或 JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(400, "请求体不能为空或格式错误");
    }

    /**
     * 处理不支持的 Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return Result.error(415, "不支持的 Content-Type，请使用 application/json");
    }

    /**
     * 处理日期格式解析错误
     */
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDateTimeParseException(DateTimeParseException e) {
        return Result.error(400, "日期格式错误，应为 YYYY-MM-DD");
    }

    /**
     * 兜底异常处理，捕获所有未被前面方法处理的其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.error(500, "服务器内部错误");
    }
}
