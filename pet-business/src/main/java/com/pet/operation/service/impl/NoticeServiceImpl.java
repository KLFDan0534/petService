package com.pet.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.common.BusinessException;
import com.pet.common.StatusCode;
import com.pet.operation.entity.Notice;
import com.pet.operation.entity.NoticeRead;
import com.pet.operation.mapper.NoticeMapper;
import com.pet.operation.mapper.NoticeReadMapper;
import com.pet.operation.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    private static final String TYPE_NOTICE = "notice";

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper, NoticeReadMapper noticeReadMapper) {
        this.noticeMapper = noticeMapper;
        this.noticeReadMapper = noticeReadMapper;
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
    public Notice getById(Long id) {
        log.info("getById() called");
        Notice n = noticeMapper.selectById(id);
        if (n == null) throw new BusinessException("公告不存在");
        return n;
    }

    @Transactional
    @Override
    public Notice create(Notice notice) {
        log.info("create() called");
        notice.setType_wsh(normalizeTypeOrDefault(notice.getType_wsh()));
        if (notice.getStatus_wsh() == null) notice.setStatus_wsh(StatusCode.NOTICE_ACTIVE.getValue());
        if (notice.getSort_order_wsh() == null) notice.setSort_order_wsh(0);
        noticeMapper.insert(notice);
        return notice;
    }

    @Transactional
    @Override
    public Notice update(Long id, Notice notice) {
        log.info("update() called");
        Notice existing = getById(id);
        if (notice.getTitle_wsh() != null) existing.setTitle_wsh(notice.getTitle_wsh());
        if (notice.getContent_wsh() != null) existing.setContent_wsh(notice.getContent_wsh());
        if (notice.getType_wsh() != null) existing.setType_wsh(normalizeTypeOrDefault(notice.getType_wsh()));
        if (notice.getImage_url_wsh() != null) existing.setImage_url_wsh(notice.getImage_url_wsh());
        if (notice.getLink_url_wsh() != null) existing.setLink_url_wsh(notice.getLink_url_wsh());
        if (notice.getSort_order_wsh() != null) existing.setSort_order_wsh(notice.getSort_order_wsh());
        if (notice.getStatus_wsh() != null) existing.setStatus_wsh(notice.getStatus_wsh());
        noticeMapper.updateById(existing);
        noticeReadMapper.delete(new LambdaQueryWrapper<NoticeRead>().eq(NoticeRead::getNotice_id_wsh, id));
        return existing;
    }

    @Transactional
    @Override
    public void markAsRead(Long id, Long userId) {
        log.info("markAsRead() called");
        Notice notice = getById(id);
        if (!TYPE_NOTICE.equals(normalizeType(notice.getType_wsh()))) {
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
        noticeMapper.deleteById(id);
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
