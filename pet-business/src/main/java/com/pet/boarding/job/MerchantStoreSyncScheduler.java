package com.pet.boarding.job;

import com.pet.boarding.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商家店铺营业状态同步定时任务。
 * <p>
 * 每分钟（60秒固定延迟）执行一次，刷新所有已审核商家的店铺营业状态。
 * <p>
 * <b>业务背景：</b>在自动营业模式下，商家的店铺状态取决于当前时间是否落在营业时段内。
 * 由于营业状态随时间动态变化（例如：09:00 开门 → 18:00 关门），需要一个定时任务
 * 定期检查并更新 store_status 字段，确保前端展示的店铺状态与时间同步。
 * <p>
 * <b>调度配置：</b>@Scheduled(fixedDelay = 60000)
 * <ul>
 *   <li>fixedDelay 表示上一次执行完成后等待 60 秒再执行下一次</li>
 *   <li>任务执行异常被捕获并记录 warn 日志，不中断后续执行</li>
 * </ul>
 * <p>
 * <b>调用链：</b>syncStoreStates() → {@link MerchantService#refreshAllStoreStates()}
 * → 逐个商家计算营业状态 → 如有变化则更新 DB 并同步看护者在线状态
 */
@Component
@Slf4j
public class MerchantStoreSyncScheduler {

    private final MerchantService merchantService;

    public MerchantStoreSyncScheduler(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 定时刷新所有商家的店铺营业状态。
     * <p>
     * <b>执行频率：</b>每 60 秒执行一次。
     * <p>
     * <b>异常处理：</b>捕获所有异常仅记录 warn 日志，防止定时任务因单次失败而中止。
     */
    @Scheduled(fixedDelay = 60000)
    public void syncStoreStates() {
        try {
            merchantService.refreshAllStoreStates();
        } catch (Exception e) {
            log.warn("syncStoreStates failed", e);
        }
    }
}
