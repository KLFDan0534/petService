package com.pet.common.annotation;

import java.lang.annotation.*;

/**
 * 业务作用：标记需要记录操作日志的 Controller 方法。 由 LogOperationAspect 通过 AOP 环绕通知自动记录： - 操作人、操作时间、请求参数、响应结果、执行时长、IP 地址
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
