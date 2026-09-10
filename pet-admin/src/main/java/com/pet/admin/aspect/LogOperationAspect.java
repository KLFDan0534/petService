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

/**
 * 【业务名称】操作日志AOP切面
 * <p>业务作用：通过AOP Around通知拦截所有标注了@LogOperation注解的方法，在方法执行前后自动记录详细的操作日志。包括模块名称、操作类型、描述、请求参数、响应体、执行耗时、操作用户、客户端IP等信息。执行失败时捕获异常信息并标记失败状态。</p>
 * <p>调用场景：所有需要记录操作日志的后台管理接口（如订单管理、用户管理、宠物管理等Controller方法）。</p>
 * <p>调用链：Controller方法执行 → @LogOperation注解命中 → LogOperationAspect.around() → joinPoint.proceed()执行原方法 → finally块中buildLog()+operationLogService.save() → 写入操作日志表</p>
 * <p>数据处理：before——记录开始时间、初始化状态为success；proceed——执行原方法、捕获响应体JSON；catch——捕获异常设置failure状态和错误消息；finally——计算耗时、buildLog构建OperationLog、调用service保存。</p>
 * <p>业务规则：@LogOperation注解的module/operation/description决定日志的业务归类；操作日志保存失败不影响原方法执行（catch Exception记录warn日志）。</p>
 * <p>状态影响：每次拦截到@LogOperation注解的方法执行，均新增一条OperationLog记录。</p>
 * <p>异常情况：原方法抛出Throwable时，切面记录失败状态后rethrow异常，不吞没；操作日志保存失败仅记录warn日志不阻断主流程。</p>
 * <p>注意事项：请求体和响应体超过2000字符会被截断；IP地址优先从X-Forwarded-For头获取（支持代理场景），其次X-Real-IP，最后getRemoteAddr()。</p>
 */
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
     * 【业务名称】操作日志环绕通知
     * <p>业务作用：拦截@LogOperation标注的方法，在执行前后记录操作日志。核心流程——记录开始时间 → 执行原方法（proceed）→ 成功时记录响应JSON → 失败时记录异常和failure状态 → finally中构建并保存OperationLog。</p>
     * <p>调用场景：任何标注了@LogOperation注解的Spring Bean方法执行时自动触发。</p>
     * <p>调用链：被拦截方法调用 → around() → joinPoint.proceed()执行原逻辑 → 成功/失败处理 → buildLog() → operationLogService.save()</p>
     * <p>数据处理：记录startTime；try块中proceed并捕获responseBody转JSON；catch块中设置status=failure并记录errorMsg；finally块中计算duration、调用buildLog组装OperationLog、调用service保存。</p>
     * <p>业务规则：原方法返回值通过toJson()转为字符串存入response_body；失败时rethrow异常不吞没；finally中日志保存异常被catch仅warn日志不中断。</p>
     * <p>状态影响：每次执行新增一条OperationLog记录。</p>
     * <p>异常情况：原方法抛出Throwable → 切面记录failure后rethrow；日志保存异常 → 记录warn后忽略。</p>
     * <p>注意事项：requestBody当前固定为null（未在前置阶段获取请求体），后续可扩展；responseBody和errorMsg均可能为null。</p>
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
                log.error("保存操作日志失败", e);
            }
        }
    }

    /**
     * 【业务名称】构建操作日志实体
     * <p>业务作用：从@LogOperation注解、Spring Security上下文、HttpServletRequest中提取信息，组装为完整的OperationLog实体。</p>
     * <p>数据来源：@LogOperation注解（module/operation/description）；SecurityContextHolder（userId/username）；RequestContextHolder（HTTP方法/URL/参数/IP）；方法执行信息（duration/status/errorMsg/responseBody）。</p>
     * <p>数据处理：通过反射获取注解属性；从SecurityContext提取Authentication和userId；从RequestContextHolder获取ServletRequestAttributes；调用getClientIp()获取真实客户端IP；responseBody和requestBody超过2000字符截断。</p>
     * <p>注意事项：requestBody当前参数为null未使用，可后续从请求中提取扩展。</p>
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

    /**
     * 【业务名称】对象转JSON字符串
     * <p>业务作用：将对象序列化为JSON字符串，用于记录请求体和响应体。</p>
     * <p>异常情况：序列化失败时返回obj.toString()作为降级。</p>
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /**
     * 【业务名称】字符串截断
     * <p>业务作用：超长字符串截断到指定长度，防止日志表中存储过大的请求/响应数据。</p>
     * <p>业务规则：null直接返回null；长度不超过maxLen直接返回；超过则截取前maxLen字符。</p>
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen);
    }

    /**
     * 【业务名称】反射提取Long类型值
     * <p>业务作用：通过反射调用目标对象的指定方法获取返回值，并转为Long类型。兼容Long/Number/String三种返回类型。</p>
     * <p>调用场景：从Authentication principal中通过getUserId()反射获取用户ID。</p>
     * <p>异常情况：反射调用失败或类型转换失败时返回null（不抛出异常）。</p>
     */
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

    /**
     * 【业务名称】获取客户端真实IP
     * <p>业务作用：从HTTP请求头中获取客户端真实IP地址，支持反向代理场景。</p>
     * <p>业务规则：优先取X-Forwarded-For头（代理环境），其次X-Real-IP头，最后getRemoteAddr()；X-Forwarded-For有多个IP时取第一个。</p>
     * <p>注意事项：X-Forwarded-For可能被伪造，若需高安全性应在网关层处理。</p>
     */
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
