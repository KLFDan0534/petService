package com.pet.membership.job;

import com.pet.membership.service.MembershipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MembershipExpirationScheduler {
    private final MembershipService membershipService;

    /**
     * 【会员到期调度器的构造注入】
     *
     * 业务作用：注入会员业务服务。
     *
     * 调用场景：Spring 容器创建 MembershipExpirationScheduler 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 membershipService。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public MembershipExpirationScheduler(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /**
     * Scheduled task that runs every 60 seconds to expire memberships
     * whose validity period has ended. Logs the count of expired memberships.
     */
    @Scheduled(fixedDelay = 60000)
    public void expireMemberships() {
        try {
            int expired = membershipService.expireMemberships();
            if (expired > 0) {
                log.info("会员到期调度器已过期 {} 个会员", expired);
            }
        } catch (Exception e) {
            log.warn("会员到期调度器执行失败", e);
        }
    }
}
