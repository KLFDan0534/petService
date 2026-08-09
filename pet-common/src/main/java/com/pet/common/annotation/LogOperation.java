package com.pet.common.annotation;

import java.lang.annotation.*;

/**
 * 【操作日志记录注解】
 *
 * 业务作用：
 * 标记需要记录操作日志的 Controller 方法。
 * 由 LogOperationAspect 通过 AOP 环绕通知自动记录：
 * - 操作人、操作时间、请求参数、响应结果、执行时长、IP 地址
 *
 * 调用场景：
 * 标注在需要审计追踪的接口方法上，如：
 * - 用户管理（创建/删除/封禁用户）
 * - 订单管理（审核/退款）
 * - 实名认证审核（通过/驳回）
 *
 * 调用链：
 * Controller(@LogOperation)
 *   ↓
 * LogOperationAspect.around()  ← AOP 拦截
 *   ↓
 * OperationLogService.save()   ← 持久化
 *
 * 注意事项：
 * - 只对 Controller 方法生效（@Target METHOD）
 * - module 建议使用业务模块名（如 user, order, real-name-review）
 * - operation 建议使用动词（如 create, update, approve, reject）
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    /** 所属模块，如 user、order、real-name-review */
    String module() default "";
    /** 操作类型，如 create、update、approve、reject */
    String operation() default "";
    /** 操作描述 */
    String description() default "";
}
