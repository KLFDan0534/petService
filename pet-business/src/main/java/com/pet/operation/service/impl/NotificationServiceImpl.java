package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NotificationMapper;
import com.pet.operation.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<Notification> listByUser(Long userId) {
        log.info("listByUser() called");
        return notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUser_id_wsh, userId)
                        .orderByDesc(Notification::getCreated_at_wsh));
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
}
