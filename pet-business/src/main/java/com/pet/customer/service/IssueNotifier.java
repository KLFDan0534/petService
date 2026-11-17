package com.pet.customer.service;

import com.pet.operation.entity.Notification;
import com.pet.operation.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 业务作用：投诉与工单共用的站内通知创建。之前这条逻辑在两个 Service 里各写了两份 （「通知发起人」和「通知商家客服」各一份），差异只有 {@code type_wsh} 和文案。
 * 业务规则：
 * <ol>
 * <li>通知只是副作用，<b>任何失败都只记 warn，不向上抛</b>——不能因为通知发不出去就让业务回滚；</li>
 * <li>文案由调用方传入，本类不拼业务措辞（投诉和工单的措辞不同，属于业务差异）；</li>
 * <li>{@code type_wsh} 决定前端角标与跳转，必须是 {@code complaint} / {@code ticket}，不允许随意改。</li>
 * </ol>
 * 注意事项：本类只负责「创建一条通知」，不负责决定「该不该通知」—— 重复通知的判定留在业务层（例如投诉处理只由 MQ 消费方发一条）。
 */
@Slf4j
@Component
public class IssueNotifier {

    private final NotificationService notificationService;
    private final MerchantCustomerServiceService merchantCustomerServiceService;

    /**
     * 【构造注入】
     *
     * <p>业务作用：注入通知服务与客服授权查询。</p>
     * <p>调用场景：Spring 容器启动创建单例时调用。</p>
     */
    public IssueNotifier(NotificationService notificationService,
                         MerchantCustomerServiceService merchantCustomerServiceService) {
        this.notificationService = notificationService;
        this.merchantCustomerServiceService = merchantCustomerServiceService;
    }

    /**
     * 【通知单个用户】
     *
     * <p>业务作用：给指定用户创建一条站内通知。</p>
     *
     * <p>业务规则：userId 为空直接返回；创建失败只记 warn。</p>
     *
     * @param type    业务类型：complaint / ticket
     * @param userId  接收人
     * @param bizId   投诉或工单 ID（写入 related_id_wsh）
     * @param title   通知标题
     * @param content 通知正文
     */
    public void notifyUser(String type, Long userId, Long bizId, String title, String content) {
        if (userId == null) {
            return;
        }
        try {
            Notification notification = new Notification();
            notification.setUser_id_wsh(userId);
            notification.setTitle_wsh(title);
            notification.setContent_wsh(content);
            notification.setType_wsh(type);
            notification.setRelated_id_wsh(bizId);
            notification.setIs_read_wsh(0);
            notificationService.create(notification);
        } catch (Exception e) {
            log.warn("发送{}通知失败: bizId={}, userId={}", type, bizId, userId, e);
        }
    }

    /**
     * 【通知商家客服】
     *
     * <p>业务作用：给某个商家下所有已授权客服各发一条通知。</p>
     *
     * <p>业务规则：商家为空或查不到客服时什么都不做；单个客服发送失败不影响其余客服。</p>
     *
     * @param type     业务类型：complaint / ticket
     * @param merchantId 商家 ID，可为空
     * @param bizId    投诉或工单 ID
     * @param title    通知标题
     * @param content  通知正文
     */
    public void notifyCustomerService(String type, Long merchantId, Long bizId,
                                      String title, String content) {
        try {
            Set<Long> csUserIds = merchantCustomerServiceService.getApprovedCsUserIds(merchantId);
            if (csUserIds == null) {
                return;
            }
            for (Long csUserId : csUserIds) {
                notifyUser(type, csUserId, bizId, title, content);
            }
        } catch (Exception e) {
            log.warn("发送{}客服提醒失败: bizId={}, merchantId={}", type, bizId, merchantId, e);
        }
    }
}
