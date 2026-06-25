package com.pet.operation.service;

import com.pet.operation.entity.Notification;

import java.util.List;

public interface NotificationService {
    /**
     * 根据用户ID获取通知列表
     * @param userId 用户ID
     * @return 通知列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<Notification> listByUser(Long userId);
    /**
     * 统计用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    long countUnread(Long userId);
    /**
     * 创建通知
     * @param notification 通知实体
     * @return 创建后的通知
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    Notification create(Notification notification);
    /**
     * 标记通知为已读
     * @param id 通知ID
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markAsRead(Long id, Long userId);
    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    void markAllAsRead(Long userId);
}

