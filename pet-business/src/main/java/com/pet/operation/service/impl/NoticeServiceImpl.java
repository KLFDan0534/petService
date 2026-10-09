package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.NoticeRead;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NoticeReadMapper;
import com.pet.operation.service.NoticeService;
import com.pet.operation.service.NotificationService;
import com.pet.mq.MessageSender;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    private static final String TYPE_NOTICE = "notice";
    private static final String DELIVERY_POPUP = "popup";
    private static final String DELIVERY_NOTIFICATION = "notification";
    private static final String DELIVERY_BROADCAST = "broadcast";

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final MessageSender messageSender;

    public NoticeServiceImpl(NoticeMapper noticeMapper, NoticeReadMapper noticeReadMapper,
                             NotificationService notificationService, UserMapper userMapper,
                             MessageSender messageSender) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.messageSender = messageSender;
    }

    /**
     * 获取所有公告列表，委托给 {@link #listAll(String)}
     */
    @Override
    @Cacheable(value = "notice", key = "'list:ALL'", unless = "#result == null || #result.isEmpty()")
    public List<Notice> listAll() {
        log.info("listAll() 被调用");
        return listAll(null);
    }

    /**
     * 根据类型筛选公告列表，类型不区分大小写
     */
    @Override
    @Cacheable(value = "notice", key = "'list:' + (#type == null ? 'ALL' : #type)", unless = "#result == null || #result.isEmpty()")
    public List<Notice> listAll(String type) {
        log.info("listAll(type) 被调用");
        String normalizedType = normalizeType(type);
        return noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .apply(normalizedType != null, "LOWER(type_wsh) = {0}", normalizedType)
                        .orderByAsc(Notice::getSort_order_wsh)
                        .orderByDesc(Notice::getCreated_at_wsh));
    }

    /**
     * 获取指定类型下所有已启用的有效公告
     */
    @Override
    @Cacheable(value = "notice", key = "'active:' + (#type == null ? 'ALL' : #type)", unless = "#result == null || #result.isEmpty()")
    public List<Notice> listActive(String type) {
        log.info("listActive() 被调用");
        String normalizedType = normalizeType(type);
        return noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus_wsh, StatusCode.NOTICE_ACTIVE.getValue())
                        .apply(normalizedType != null, "LOWER(type_wsh) = {0}", normalizedType)
                        .orderByAsc(Notice::getSort_order_wsh)
                        .orderByDesc(Notice::getCreated_at_wsh));
    }

    /**
     * 获取用户未读的公告列表（仅 "notice" 类型）
     */
    @Override
    public List<Notice> listUnread(Long userId) {
        log.info("listUnread() 被调用");
        return noticeMapper.selectUnreadByUser(userId, TYPE_NOTICE, StatusCode.NOTICE_ACTIVE.getValue());
    }

    /**
     * 获取用户尚未关闭的弹窗公告列表（投递方式含 "popup"）
     * <p>
     * 懒加载模型：notice_read_wsh 表中「有记录 = 用户已关闭该弹窗 = 不再弹」，
     * 「无记录 = 用户还没见过 = 需要弹」。因此用 NOT EXISTS 排除已有记录的公告，
     * 不再依赖 is_read_wsh 字段做状态判断。
     */
    @Override
    public List<Notice> listPopup(Long userId) {
        log.info("listPopup() 被调用");
        return noticeMapper.selectPopupByUser(userId, StatusCode.NOTICE_ACTIVE.getValue());
    }

    /**
     * 记录用户已关闭弹窗公告，如果已有记录则忽略
     * <p>
     * 懒加载模型：用户关闭弹窗时，才为该用户插入一条阅读记录（"有记录 = 已读"）。
     * 依靠唯一索引 uk_notice_read_notice_user 保证并发下不会重复插入。
     */
    @Override
    @Transactional
    public void dismissPopup(Long id, Long userId) {
        log.info("dismissPopup() 被调用");

        if(userMapper.selectById(userId) == null) return;

        // 1. 幂等：该用户对该公告已有记录（说明之前已关闭过），直接返回
        Long count = noticeReadMapper.selectCount(new LambdaQueryWrapper<NoticeRead>()
                .eq(NoticeRead::getNotice_id_wsh, id)
                .eq(NoticeRead::getUser_id_wsh, userId));
        if (count != null && count > 0) return;

        // 2. 无记录 → 首次关闭，插入一条已读记录（记录存在即代表已读）
        NoticeRead read = new NoticeRead();
        read.setNotice_id_wsh(id);
        read.setUser_id_wsh(userId);
        read.setRead_at_wsh(LocalDateTime.now());
        try {
            noticeReadMapper.insert(read);
        } catch (DuplicateKeyException ignored) {
            // 并发场景下两次请求同时插入，靠唯一索引兜底，忽略即可
        }
    }

    /**
     * 根据主键获取公告，不存在时抛出异常
     */
    @Override
    @Cacheable(value = "notice", key = "'id:' + #id", unless = "#result == null")
    public Notice getById(Long id) {
        log.info("getById() 被调用");
        Notice n = noticeMapper.selectById(id);
        if (n == null) throw new BusinessException("公告不存在");
        return n;
    }

    /**
     * 创建公告，校验类型与投递方式，并按投递方式为用户生成通知
     */
    @Transactional
//    TODO 需要区分公告是否有选择消息通知,如果没选择应该是不需要清空通知的缓存的
    @Caching(evict = {
        @CacheEvict(value = "notice", allEntries = true),
        @CacheEvict(value = "notification", allEntries = true),
        @CacheEvict(value = "notificationUnreadCount", allEntries = true)
    })
    @Override
    public Notice create(NoticeCreateRequestDTO request) {
        log.info("create() 被调用");
        Notice notice = new Notice();
        String type = normalizeTypeOrDefault(request.getType_wsh());
        String content = request.getContent_wsh();
        // 弹窗或是消息
        String deliveryType = normalizeDeliveryType(request.getDelivery_type_wsh());

        if (isNoticeType(type)) {
            requireText(content, "公告内容不能为空");
            validateDeliveryTypeForNotice(deliveryType);
        } else {
            content = content == null ? "" : content;
            deliveryType = "";
        }

        notice.setTitle_wsh(request.getTitle_wsh());
        notice.setContent_wsh(content);
        notice.setType_wsh(type);
        notice.setDelivery_type_wsh(deliveryType);
        notice.setImage_url_wsh(request.getImage_url_wsh());
        notice.setLink_url_wsh(request.getLink_url_wsh());
        notice.setSort_order_wsh(request.getSort_order_wsh() != null ? request.getSort_order_wsh() : 0);
        notice.setStatus_wsh(request.getStatus_wsh() != null ? request.getStatus_wsh() : StatusCode.NOTICE_ACTIVE.getValue());
        noticeMapper.insert(notice);

        // 通知生成改为异步：只投递公告ID，由 MQ 消费者在后台批量生成
        if (shouldCreateNotifications(notice)) {
            messageSender.sendNoticeNotification(notice.getId_wsh());
        }
        return notice;
    }

    /**
     * 更新公告，清空旧已读记录并重新同步通知
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notice", allEntries = true),
        @CacheEvict(value = "notification", allEntries = true),
        @CacheEvict(value = "notificationUnreadCount", allEntries = true)
    })
    @Override
    public Notice update(Long id, NoticeUpdateRequestDTO request) {
        log.info("update() 被调用");
        Notice existing = getById(id);
        if (request.getTitle_wsh() != null) existing.setTitle_wsh(request.getTitle_wsh());
        if (request.getContent_wsh() != null) existing.setContent_wsh(request.getContent_wsh());
        if (request.getType_wsh() != null) existing.setType_wsh(normalizeTypeOrDefault(request.getType_wsh()));
        if (request.getDelivery_type_wsh() != null) existing.setDelivery_type_wsh(normalizeDeliveryType(request.getDelivery_type_wsh()));
        if (request.getImage_url_wsh() != null) existing.setImage_url_wsh(request.getImage_url_wsh());
        if (request.getLink_url_wsh() != null) existing.setLink_url_wsh(request.getLink_url_wsh());
        if (request.getSort_order_wsh() != null) existing.setSort_order_wsh(request.getSort_order_wsh());
        if (request.getStatus_wsh() != null) existing.setStatus_wsh(request.getStatus_wsh());

        String type = normalizeTypeOrDefault(existing.getType_wsh());
        existing.setType_wsh(type);
        existing.setDelivery_type_wsh(normalizeDeliveryType(existing.getDelivery_type_wsh()));
        if (isNoticeType(type)) {
            requireText(existing.getContent_wsh(), "公告内容不能为空");
            validateDeliveryTypeForNotice(existing.getDelivery_type_wsh());
        } else {
            if (existing.getContent_wsh() == null) existing.setContent_wsh("");
            existing.setDelivery_type_wsh("");
        }

        noticeMapper.updateById(existing);
        noticeReadMapper.delete(new LambdaQueryWrapper<NoticeRead>().eq(NoticeRead::getNotice_id_wsh, id));
        syncNoticeNotifications(existing);
        return existing;
    }

    /**
     * 标记公告为已读，仅 "notice" 类型支持此操作
     */
    @Transactional
    @Override
    public void markAsRead(Long id, Long userId) {
        log.info("markAsRead() 被调用");
        Notice notice = getById(id);
        if (!isNoticeType(notice.getType_wsh())) {
            throw new BusinessException("仅公告类型可标记为已读");
        }

        NoticeRead existing = noticeReadMapper.selectOne(
                new LambdaQueryWrapper<NoticeRead>()
                        .eq(NoticeRead::getNotice_id_wsh, id)
                        .eq(NoticeRead::getUser_id_wsh, userId)
                        .last("LIMIT 1"));
        if (existing != null) return;

        NoticeRead read = new NoticeRead();
        read.setNotice_id_wsh(id);
        read.setUser_id_wsh(userId);
        read.setRead_at_wsh(LocalDateTime.now());
        try {
            noticeReadMapper.insert(read);
        } catch (DuplicateKeyException ignored) {
        }
    }

    /**
     * 物理删除公告及其关联的已读记录和通知
     */
    @Transactional
    @CacheEvict(value = "notice", allEntries = true)
    @Override
    public void delete(Long id) {
        log.info("delete() 被调用");
        noticeReadMapper.delete(new LambdaQueryWrapper<NoticeRead>().eq(NoticeRead::getNotice_id_wsh, id));
        notificationService.deleteByRelatedId(id);
        noticeMapper.deleteById(id);
    }

    private void syncNoticeNotifications(Notice notice) {
        Long noticeId = notice.getId_wsh();
        if (noticeId == null) return;

        notificationService.deleteByRelatedId(noticeId);
        if (shouldCreateNotifications(notice)) {
            messageSender.sendNoticeNotification(noticeId);
        }
    }

//    通知状态验证
    private boolean shouldCreateNotifications(Notice notice) {
        return isNoticeType(notice.getType_wsh())
                && Integer.valueOf(StatusCode.NOTICE_ACTIVE.getValue()).equals(notice.getStatus_wsh())
                && (deliveryIncludes(notice.getDelivery_type_wsh(), DELIVERY_NOTIFICATION)
                || deliveryIncludes(notice.getDelivery_type_wsh(), DELIVERY_BROADCAST));
    }

    private void validateDeliveryTypeForNotice(String deliveryType) {
        if (deliveryType == null || deliveryType.isBlank()) {
            throw new BusinessException("必须至少选择一种投递方式");
        }
        boolean allValid = Arrays.stream(deliveryType.split(","))
                .allMatch(this::isAllowedDeliveryType);
        if (!allValid) {
            throw new BusinessException("投递方式不正确");
        }
    }

    private boolean isAllowedDeliveryType(String deliveryType) {
        return DELIVERY_POPUP.equals(deliveryType)
                || DELIVERY_NOTIFICATION.equals(deliveryType)
                || DELIVERY_BROADCAST.equals(deliveryType);
    }

    private boolean deliveryIncludes(String deliveryType, String target) {
        if (deliveryType == null || deliveryType.isBlank()) return false;
        return Arrays.stream(deliveryType.split(","))
                .map(String::trim)
                .anyMatch(target::equals);
    }

    private String normalizeDeliveryType(String deliveryType) {
        if (deliveryType == null || deliveryType.isBlank()) return "";
        return Arrays.stream(deliveryType.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(message);
    }

    private boolean isNoticeType(String type) {
        return TYPE_NOTICE.equals(normalizeType(type));
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return null;
        return type.trim().toLowerCase();
    }

    private String normalizeTypeOrDefault(String type) {
        String normalized = normalizeType(type);
        return normalized == null ? TYPE_NOTICE : normalized;
    }
}
