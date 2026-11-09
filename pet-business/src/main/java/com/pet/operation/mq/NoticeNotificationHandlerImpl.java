package com.pet.operation.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.StatusCode;
import com.pet.common.mq.NoticeNotificationHandler;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NotificationMapper;
import com.pet.operation.service.NotificationBroadcaster;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【公告通知异步处理实现】
 *
 * 业务作用：
 * 消费 MQ 队列 notice.notification 投递的公告ID，在后台为该公告
 * 批量创建全量用户的站内通知，并向在线用户推送 SSE。
 *
 * 为什么放 pet-business：
 * 接口 NoticeNotificationHandler 定义在 pet-common，供 pet-framework 的
 * MessageListener 编译期引用；实现由本模块提供，运行时由 Spring 注入。
 * 这样 pet-framework 不需要（也不能）反向依赖 pet-business。
 *
 * 调用链：
 * NoticeServiceImpl.create()/update() → MessageSender.sendNoticeNotification()
 *   ↓ 队列 notice.notification
 * MessageListener.handleNoticeNotification()
 *   ↓
 * NoticeNotificationHandlerImpl.handle(noticeId)
 *
 * 状态影响：
 * - 新增多条 notification_wsh 记录（该公告 × 全部正常状态用户）
 * - 清空 notification 缓存（跨用户，无法定位单个 key）
 *
 * 异常情况：
 * 公告不存在或状态不满足时静默跳过，仅记录日志，不抛异常。
 *
 * 注意事项：
 * - 消息只携带 noticeId，标题/内容等以数据库当前值为准，避免消息里的数据过期
 * - 当前为「基本可用」版本：一次性载入全量用户，用户量极大时存在 OOM 风险，
 *   后续需改为分页流式处理
 * - 当前未做幂等保护：MQ 只保证至少一次投递，重复消费会产生重复通知，
 *   后续需配合唯一索引 + INSERT IGNORE 实现幂等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeNotificationHandlerImpl implements NoticeNotificationHandler {

    /** 公告通知的类型标识 */
    private static final String TYPE_NOTICE = "notice";
    private static final String DELIVERY_NOTIFICATION = "notification";
    private static final String DELIVERY_BROADCAST = "broadcast";
    /** 通知批量插入的单批条数，避免单条 SQL 超出 MySQL max_allowed_packet */
    private static final int NOTIFICATION_BATCH_SIZE = 500;

    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final NotificationBroadcaster notificationBroadcaster;

    /**
     * 为指定公告生成全量用户通知
     * <p>
     * 消费端会重新查询公告并用最新状态校验：公告可能在消息排队期间被删除、
     * 改为草稿或改掉投递方式，此时应跳过而不报错。
     *
     * @param noticeId 公告ID
     */
    @Override
    @CacheEvict(value = "notification", allEntries = true)
    public void handle(Long noticeId) {
        if (noticeId == null) {
            log.warn("公告通知事件缺少 noticeId，消息跳过");
            return;
        }

        // 1. 重新查公告：消息排队期间公告可能已被删除
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            log.warn("公告不存在，跳过通知生成: {}", noticeId);
            return;
        }

        // 2. 用最新状态复核：可能已被改为草稿或改掉投递方式
        if (!shouldCreateNotifications(notice)) {
            log.info("公告 {} 当前状态无需生成通知，跳过", noticeId);
            return;
        }

        // 3. 只通知状态正常的用户
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus_wsh, StatusCode.USER_ACTIVE.getValue()));
        if (users == null || users.isEmpty()) {
            log.info("公告 {} 无可用用户，跳过", noticeId);
            return;
        }

        // 4. 组装通知对象（内存操作，不进数据库）
        List<Notification> notifications = users.stream().map(user -> {
            Notification notification = new Notification();
            notification.setUser_id_wsh(user.getId_wsh());
            notification.setTitle_wsh(notice.getTitle_wsh());
            notification.setContent_wsh(notice.getContent_wsh());
            notification.setType_wsh(TYPE_NOTICE);
            notification.setIs_read_wsh(StatusCode.NOTIFICATION_UNREAD.getValue());
            notification.setRelated_id_wsh(noticeId);
            return notification;
        }).collect(Collectors.toList());

        // 5. 分批批量插入，避免单条 SQL 超出 max_allowed_packet
        int total = 0;
        for (int i = 0; i < notifications.size(); i += NOTIFICATION_BATCH_SIZE) {
            List<Notification> batch = notifications.subList(
                    i, Math.min(i + NOTIFICATION_BATCH_SIZE, notifications.size()));
            total += notificationMapper.insertBatch(batch);
        }
        log.info("公告 {} 通知批量插入完成，共 {} 条", noticeId, total);

        // 6. 插入完成后推送；离线用户由 broadcaster 内部自动跳过
        for (Notification notification : notifications) {
            notificationBroadcaster.broadcast(notification.getUser_id_wsh(), notification);
        }
    }

    /**
     * 判断公告是否需要生成用户通知
     * <p>
     * 条件：类型为 notice、状态为已发布、且投递方式包含 notification 或 broadcast。
     */
    private boolean shouldCreateNotifications(Notice notice) {
        return TYPE_NOTICE.equals(normalizeType(notice.getType_wsh()))
                && Integer.valueOf(StatusCode.NOTICE_ACTIVE.getValue()).equals(notice.getStatus_wsh())
                && (deliveryIncludes(notice.getDelivery_type_wsh(), DELIVERY_NOTIFICATION)
                || deliveryIncludes(notice.getDelivery_type_wsh(), DELIVERY_BROADCAST));
    }

    /**
     * 【归一化公告类型】
     *
     * 业务作用：去除空白并转小写，便于与常量比较。
     *
     * 调用场景：shouldCreateNotifications 判断公告类型是否为 notice。
     *
     * 调用链：handle → shouldCreateNotifications → normalizeType。
     *
     * 数据处理：null 或空白返回 null，否则 trim 后转小写。
     *
     * 业务规则：空白与 null 等价。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：返回 null 后与 TYPE_NOTICE 比较恒为 false，公告会被跳过。
     */
    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return null;
        return type.trim().toLowerCase();
    }

    /**
     * 【判断投递方式是否包含指定渠道】
     *
     * 业务作用：在逗号分隔的投递方式串中查找目标渠道。
     *
     * 调用场景：shouldCreateNotifications 判断是否含 notification 或 broadcast。
     *
     * 调用链：handle → shouldCreateNotifications → deliveryIncludes。
     *
     * 数据处理：按逗号切分，逐项 trim 后与目标值做精确相等比较。
     *
     * 业务规则：null 或空白投递方式返回 false；比较区分大小写。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：未对空元素过滤，但空串不会等于目标值，因此不影响结果。
     */
    private boolean deliveryIncludes(String deliveryType, String target) {
        if (deliveryType == null || deliveryType.isBlank()) return false;
        return Arrays.stream(deliveryType.split(","))
                .map(String::trim)
                .anyMatch(target::equals);
    }
}
