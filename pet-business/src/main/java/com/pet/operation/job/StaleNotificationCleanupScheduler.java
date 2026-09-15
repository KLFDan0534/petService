package com.pet.operation.job;

import com.pet.operation.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 失效公告类通知的清理调度器。
 * <p>
 * 通知列表的读路径（{@code listByUser}）只做过滤、不做删除，
 * 真实清理统一由本调度器承担，避免在 {@code @Cacheable} 方法体内写数据库。
 * <p>
 * 清理完成后会清空通知相关缓存，保证用户能立刻看到清理结果。
 */
@Component
@Slf4j
public class StaleNotificationCleanupScheduler {

    private final NotificationService notificationService;

    public StaleNotificationCleanupScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 每小时清理一次因公告被删除或停用而产生的失效通知。
     * <p>
     * 公告变更时本就会通过 {@code deleteByRelatedId} 主动清理，
     * 这里主要兜底历史脏数据与状态漂移。
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 120000)
    public void purgeStaleNoticeNotifications() {
        try {
            int purged = notificationService.purgeStaleNoticeNotifications();
            if (purged > 0) {
                log.info("失效通知清理调度器已清理 {} 条失效通知", purged);
            }
        } catch (Exception e) {
            log.warn("失效通知清理调度器执行失败", e);
        }
    }
}
