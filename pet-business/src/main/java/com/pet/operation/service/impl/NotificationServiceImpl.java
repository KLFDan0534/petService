package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.PageRequestDTO;
import com.pet.common.StatusCode;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NotificationMapper;
import com.pet.operation.service.NotificationBroadcaster;
import com.pet.operation.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NoticeMapper noticeMapper;
    private final NotificationBroadcaster notificationBroadcaster;

    public NotificationServiceImpl(NotificationMapper notificationMapper, NoticeMapper noticeMapper,
                                   NotificationBroadcaster notificationBroadcaster) {
        this.notificationMapper = notificationMapper;
        this.noticeMapper = noticeMapper;
        this.notificationBroadcaster = notificationBroadcaster;
    }

    /**
     * 查询用户的所有通知，创建时间倒序。自动清理已失效的公告类通知
     */
    @Override
    public List<Notification> listByUser(Long userId) {
        log.info("listByUser() 被调用");
        List<Notification> list = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .orderByDesc(Notification::getCreated_at_wsh));

        List<Notification> noticeNotifs = list.stream()
                .filter(n -> "notice".equals(n.getType_wsh()) && n.getRelated_id_wsh() != null)
                .collect(Collectors.toList());

        if (!noticeNotifs.isEmpty()) {
            List<Long> noticeIds = noticeNotifs.stream()
                    .map(Notification::getRelated_id_wsh)
                    .collect(Collectors.toList());

            Set<Long> activeIds = noticeMapper.selectList(
                    new LambdaQueryWrapper<Notice>()
                            .in(Notice::getId_wsh, noticeIds)
                            .eq(Notice::getDeleted_wsh, 0)
                            .eq(Notice::getStatus_wsh, StatusCode.NOTICE_ACTIVE.getValue())
                            .select(Notice::getId_wsh))
                    .stream().map(Notice::getId_wsh).collect(Collectors.toSet());

            List<Long> staleIds = noticeNotifs.stream()
                    .filter(n -> !activeIds.contains(n.getRelated_id_wsh()))
                    .map(Notification::getId_wsh)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!staleIds.isEmpty()) {
                log.info("清理 {} 条因通知被删除或停用而产生的过期通知", staleIds.size());
                notificationMapper.delete(
                        new LambdaQueryWrapper<Notification>()
                                .in(Notification::getId_wsh, staleIds));
                list.removeIf(n -> staleIds.contains(n.getId_wsh()));
            }
        }

        return list;
    }

    /**
     * 统计用户未读通知数量
     */
    @Override
    @Cacheable(value = "notice", key = "'unreadCount:' + #userId", unless = "#userId == null")
    public long countUnread(Long userId) {
        log.info("countUnread() 被调用");
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .eq(Notification::getIs_read_wsh, 0));
    }

    /**
     * 创建通知，默认未读，并通过 SSE 实时推送给目标用户
     */
    @Transactional
    @Override
    public Notification create(Notification notification) {
        log.info("create() 被调用");
        notification.setIs_read_wsh(0);
        notificationMapper.insert(notification);
        notificationBroadcaster.broadcast(notification.getUser_id_wsh(), notification);
        return notification;
    }

    /**
     * 标记指定通知为已读。仅该通知属于当前用户时才生效
     */
    @Transactional
    @Override
    public void markAsRead(Long id, Long userId) {
        log.info("markAsRead() 被调用");
        Notification notif = notificationMapper.selectById(id);
        if (notif != null && notif.getUser_id_wsh().equals(userId)) {
            notif.setIs_read_wsh(1);
            notificationMapper.updateById(notif);
        }
    }

    /**
     * 将用户所有未读通知一次性标记为已读
     */
    @Transactional
    @Override
    public void markAllAsRead(Long userId) {
        log.info("markAllAsRead() 被调用");
        notificationMapper.update(
                new Notification() {{ setIs_read_wsh(1); }},
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .eq(Notification::getIs_read_wsh, 0));
    }

    /**
     * 根据关联业务 ID 删除所有通知（如公告更新时清理旧通知）
     */
    @Transactional
    @Override
    public void deleteByRelatedId(Long relatedId) {
        log.info("deleteByRelatedId() 被调用");
        notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRelated_id_wsh, relatedId));
    }

    /**
     * 管理员分页查询全部通知，支持按类型及已读状态精确筛选
     */
    @Override
    public IPage<Notification> pageAll(PageRequestDTO pageParam, String type, Integer isRead) {
        log.info("pageAll() 被调用");
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(StringUtils.hasText(type), Notification::getType_wsh, type)
                .eq(isRead != null, Notification::getIs_read_wsh, isRead)
                .orderByDesc(Notification::getCreated_at_wsh);
        return notificationMapper.selectPage(new Page<>(pageParam.getPage(), pageParam.getSize()), wrapper);
    }
}
