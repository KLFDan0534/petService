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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    /** 用户通知列表缓存名 */
    private static final String CACHE_NOTIFICATION = "notification";
    /** 未读通知数量缓存名 */
    private static final String CACHE_UNREAD_COUNT = "notificationUnreadCount";

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
     * 查询用户的所有通知，创建时间倒序。自动过滤已失效的公告类通知
     */
    @Override
    @Cacheable(value = CACHE_NOTIFICATION, key = "#userId", unless = "#userId == null")
    public List<Notification> listByUser(Long userId) {
        log.debug("listByUser() 被调用");
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
                log.debug("已失效公告类通知 {} 条未从列表返回", staleIds.size());
                list = list.stream()
                        .filter(n -> !staleIds.contains(n.getId_wsh()))
                        .collect(Collectors.toList());
            }
        }

        return list;
    }

    /**
     * 清理所有已失效的公告类通知（关联公告已被删除或停用）
     * <p>
     * 该方法为写操作，已从 {@link #listByUser(Long)} 的缓存读路径中剥离，
     * 由定时任务调用，避免「读缓存的方法里写数据库」以及缓存无法自洁的问题。
     *
     * @return 实际清理的通知条数
     */
    @Transactional
    @Override
    @CacheEvict(value = CACHE_NOTIFICATION, allEntries = true)
    public int purgeStaleNoticeNotifications() {
        List<Notification> noticeNotifs = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType_wsh, "notice")
                        .isNotNull(Notification::getRelated_id_wsh));

        if (noticeNotifs.isEmpty()) {
            return 0;
        }

        List<Long> noticeIds = noticeNotifs.stream()
                .map(Notification::getRelated_id_wsh)
                .distinct() // 去重
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

        if (staleIds.isEmpty()) {
            return 0;
        }

        log.info("清理 {} 条因公告被删除或停用而产生的过期通知", staleIds.size());
        // 分批删除，避免 IN 条件过长
        for (int i = 0; i < staleIds.size(); i += 500) {
            List<Long> batch = new ArrayList<>(staleIds.subList(i, Math.min(i + 500, staleIds.size())));
            notificationMapper.delete(
                    new LambdaQueryWrapper<Notification>()
                            .in(Notification::getId_wsh, batch));
        }
        return staleIds.size();
    }

    /**
     * 统计用户未读通知数量
     */
    @Override
    @Cacheable(value = CACHE_UNREAD_COUNT, key = "#userId", unless = "#userId == null")
    public long countUnread(Long userId) {
        log.debug("countUnread() 被调用");
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
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICATION, key = "#notification?.user_id_wsh", condition = "#notification?.user_id_wsh != null"),
            @CacheEvict(value = CACHE_UNREAD_COUNT, key = "#notification?.user_id_wsh", condition = "#notification?.user_id_wsh != null")
    })
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
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICATION, key = "#userId", condition = "#userId != null"),
            @CacheEvict(value = CACHE_UNREAD_COUNT, key = "#userId", condition = "#userId != null")
    })
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
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICATION, key = "#userId", condition = "#userId != null"),
            @CacheEvict(value = CACHE_UNREAD_COUNT, key = "#userId", condition = "#userId != null")
    })
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
     * <p>
     * 删除范围跨越多个用户，无法定位到单个缓存 key，故整体清空通知相关缓存。
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICATION, allEntries = true),
            @CacheEvict(value = CACHE_UNREAD_COUNT, allEntries = true)
    })
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
