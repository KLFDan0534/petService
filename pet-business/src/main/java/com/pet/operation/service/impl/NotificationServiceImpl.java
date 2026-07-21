package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.StatusCode;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NotificationMapper;
import com.pet.operation.service.NotificationBroadcaster;
import com.pet.operation.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<Notification> listByUser(Long userId) {
        log.info("listByUser() called");
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
                log.info("cleaning up {} stale notification(s) for deleted/inactive notices", staleIds.size());
                notificationMapper.delete(
                        new LambdaQueryWrapper<Notification>()
                                .in(Notification::getId_wsh, staleIds));
                list.removeIf(n -> staleIds.contains(n.getId_wsh()));
            }
        }

        return list;
    }

    @Override
    public long countUnread(Long userId) {
        log.info("countUnread() called");
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .eq(Notification::getIs_read_wsh, 0));
    }

    @Transactional
    @Override
    public Notification create(Notification notification) {
        log.info("create() called");
        notification.setIs_read_wsh(0);
        notificationMapper.insert(notification);
        notificationBroadcaster.broadcast(notification.getUser_id_wsh(), notification);
        return notification;
    }

    @Transactional
    @Override
    public void markAsRead(Long id, Long userId) {
        log.info("markAsRead() called");
        Notification notif = notificationMapper.selectById(id);
        if (notif != null && notif.getUser_id_wsh().equals(userId)) {
            notif.setIs_read_wsh(1);
            notificationMapper.updateById(notif);
        }
    }

    @Transactional
    @Override
    public void markAllAsRead(Long userId) {
        log.info("markAllAsRead() called");
        notificationMapper.update(
                new Notification() {{ setIs_read_wsh(1); }},
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .eq(Notification::getIs_read_wsh, 0));
    }

    @Transactional
    @Override
    public void deleteByRelatedId(Long relatedId) {
        log.info("deleteByRelatedId() called");
        notificationMapper.delete(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRelated_id_wsh, relatedId));
    }
}
