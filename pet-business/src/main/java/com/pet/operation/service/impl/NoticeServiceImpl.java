package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.operation.dto.NoticeCreateRequestDTO;
import com.pet.operation.dto.NoticeUpdateRequestDTO;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.NoticeRead;
import com.pet.operation.entity.Notification;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NoticeReadMapper;
import com.pet.operation.service.NoticeService;
import com.pet.operation.service.NotificationService;
import com.pet.system.entity.User;
import com.pet.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
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

    public NoticeServiceImpl(NoticeMapper noticeMapper, NoticeReadMapper noticeReadMapper,
                             NotificationService notificationService, UserMapper userMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
    }

    @Override
    public List<Notice> listAll() {
        log.info("listAll() called");
        return listAll(null);
    }

    @Override
    public List<Notice> listAll(String type) {
        log.info("listAll(type) called");
        String normalizedType = normalizeType(type);
        return noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .apply(normalizedType != null, "LOWER(type_wsh) = {0}", normalizedType)
                        .orderByAsc(Notice::getSort_order_wsh)
                        .orderByDesc(Notice::getCreated_at_wsh));
    }

    @Override
    public List<Notice> listActive(String type) {
        log.info("listActive() called");
        String normalizedType = normalizeType(type);
        return noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus_wsh, StatusCode.NOTICE_ACTIVE.getValue())
                        .apply(normalizedType != null, "LOWER(type_wsh) = {0}", normalizedType)
                        .orderByAsc(Notice::getSort_order_wsh)
                        .orderByDesc(Notice::getCreated_at_wsh));
    }

    @Override
    public List<Notice> listUnread(Long userId) {
        log.info("listUnread() called");
        return noticeMapper.selectUnreadByUser(userId, TYPE_NOTICE, StatusCode.NOTICE_ACTIVE.getValue());
    }

    @Override
    public List<Notice> listPopup(Long userId) {
        log.info("listPopup() called");
        return noticeMapper.selectPopupByUser(userId, StatusCode.NOTICE_ACTIVE.getValue());
    }

    @Override
    public void dismissPopup(Long id, Long userId) {
        log.info("dismissPopup() called");
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

    @Override
    public Notice getById(Long id) {
        log.info("getById() called");
        Notice n = noticeMapper.selectById(id);
        if (n == null) throw new BusinessException("公告不存在");
        return n;
    }

    @Transactional
    @Override
    public Notice create(NoticeCreateRequestDTO request) {
        log.info("create() called");
        Notice notice = new Notice();
        String type = normalizeTypeOrDefault(request.getType_wsh());
        String content = request.getContent_wsh();
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

        createNoticeNotifications(notice);
        return notice;
    }

    @Transactional
    @Override
    public Notice update(Long id, NoticeUpdateRequestDTO request) {
        log.info("update() called");
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

    @Transactional
    @Override
    public void markAsRead(Long id, Long userId) {
        log.info("markAsRead() called");
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

    @Transactional
    @Override
    public void delete(Long id) {
        log.info("delete() called");
        noticeReadMapper.delete(new LambdaQueryWrapper<NoticeRead>().eq(NoticeRead::getNotice_id_wsh, id));
        notificationService.deleteByRelatedId(id);
        noticeMapper.deleteById(id);
    }

    private void syncNoticeNotifications(Notice notice) {
        Long noticeId = notice.getId_wsh();
        if (noticeId == null) return;

        notificationService.deleteByRelatedId(noticeId);
        createNoticeNotifications(notice);
    }

    private void createNoticeNotifications(Notice notice) {
        Long noticeId = notice.getId_wsh();
        if (noticeId == null || !shouldCreateNotifications(notice)) return;

        log.info("broadcasting notice {} to all users", noticeId);
        List<User> users = userMapper.selectList(null);
        if (users == null || users.isEmpty()) return;

        users.forEach(user -> {
            Notification notification = new Notification();
            notification.setUser_id_wsh(user.getId_wsh());
            notification.setTitle_wsh(notice.getTitle_wsh());
            notification.setContent_wsh(notice.getContent_wsh());
            notification.setType_wsh(TYPE_NOTICE);
            notification.setRelated_id_wsh(noticeId);
            notificationService.create(notification);
        });
    }

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