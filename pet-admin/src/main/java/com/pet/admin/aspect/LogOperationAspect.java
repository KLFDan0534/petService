package com.pet.admin.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.StatusCode;
import com.pet.common.annotation.LogOperation;
import com.pet.operation.entity.OperationLog;
import com.pet.operation.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class LogOperationAspect {

    private static final Logger log = LoggerFactory.getLogger(LogOperationAspect.class);
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public LogOperationAspect(OperationLogService operationLogService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕通知，记录操作日志
     */
    @Around("@annotation(com.pet.common.annotation.LogOperation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String requestBody = null;
        String responseBody = null;
        String errorMsg = null;
        int status = StatusCode.LOG_SUCCESS.getValue();

        try {
            Object result = joinPoint.proceed();
            responseBody = toJson(result);
            return result;
        } catch (Throwable e) {
            status = StatusCode.LOG_FAILURE.getValue();
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;
                OperationLog opLog = buildLog(joinPoint, duration, status, errorMsg, requestBody, responseBody);
                operationLogService.save(opLog);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }
    }

    /**
     * 构建操作日志
     */
    private OperationLog buildLog(ProceedingJoinPoint joinPoint, long duration, int status,
                                  String errorMsg, String requestBody, String responseBody) {
        OperationLog opLog = new OperationLog();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation annotation = method.getAnnotation(LogOperation.class);

        // from annotation
        opLog.setModule_wsh(annotation.module());
        opLog.setOperation_wsh(annotation.operation());
        opLog.setDescription_wsh(annotation.description());

        // from security  context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Long userId = extractLong(auth.getPrincipal(), "getUserId");
            if (userId != null) {
                opLog.setUser_id_wsh(userId);
            }
            opLog.setUsername_wsh(auth.getName());
        }

        // from HttpServletRequest
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            opLog.setMethod_wsh(request.getMethod());
            opLog.setRequest_url_wsh(request.getRequestURI());
            opLog.setRequest_params_wsh(toJson(request.getParameterMap()));
            opLog.setIp_address_wsh(getClientIp(request));
        }

        opLog.setDuration_wsh(duration);
        opLog.setStatus_wsh(status);
        opLog.setError_msg_wsh(errorMsg);

        if (requestBody != null) opLog.setRequest_body_wsh(truncate(requestBody, 2000));
        if (responseBody != null) opLog.setResponse_body_wsh(truncate(responseBody, 2000));

        return opLog;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }

    private Long extractLong(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Long longValue) return longValue;
            if (value instanceof Number number) return number.longValue();
            if (value instanceof String text) return Long.parseLong(text);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
