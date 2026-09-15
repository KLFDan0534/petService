package com.pet.customer.job;

import com.pet.customer.service.MerchantCustomerServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商家客服申请超时自动拒绝定时任务。
 * <p>
 * <b>业务背景：</b>客服申请的唯一审核人是被申请的商家（管理员审核通道已移除）。
 * 若商家长期不处理，申请会永久停留在 pending，申请人拿不到结果。本任务为平台提供兜底：
 * 停留超过配置天数的 pending 申请由系统自动置为 rejected。
 * <p>
 * <b>调度配置：</b>@Scheduled(cron = "0 30 3 * * ?")，每天 03:30 执行一次。
 * <ul>
 *   <li>选择每日低峰时段执行，避免与商家操作竞争</li>
 *   <li>任务执行异常被捕获并记录 warn 日志，不中断后续执行</li>
 * </ul>
 * <p>
 * <b>可配置项：</b>merchant.customer-service.pending-timeout-days（默认 7 天）。
 * <p>
 * <b>调用链：</b>autoRejectStaleApplications() → {@link MerchantCustomerServiceService#autoRejectStalePending(int)}
 */
@Component
@Slf4j
public class MerchantCustomerServiceTimeoutScheduler {

    private final MerchantCustomerServiceService merchantCustomerServiceService;
    private final int pendingTimeoutDays;

    public MerchantCustomerServiceTimeoutScheduler(
            MerchantCustomerServiceService merchantCustomerServiceService,
            @Value("${merchant.customer-service.pending-timeout-days:7}") int pendingTimeoutDays) {
        this.merchantCustomerServiceService = merchantCustomerServiceService;
        this.pendingTimeoutDays = pendingTimeoutDays;
    }

    /**
     * 自动拒绝商家超时未处理的客服申请。
     * <p>
     * <b>执行频率：</b>每天 03:30 执行一次。
     * <p>
     * <b>异常处理：</b>捕获所有异常仅记录 warn 日志，防止定时任务因单次失败而中止。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void autoRejectStaleApplications() {
        try {
            int rejected = merchantCustomerServiceService.autoRejectStalePending(pendingTimeoutDays);
            if (rejected > 0) {
                log.info("客服申请超时自动拒绝完成，timeoutDays={}, count={}", pendingTimeoutDays, rejected);
            }
        } catch (Exception e) {
            log.warn("客服申请超时自动拒绝失败", e);
        }
    }
}
