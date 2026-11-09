package com.pet.operation.service.impl;

import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.BusinessException;
import com.pet.common.PageRequestDTO;
import com.pet.common.ReviewStatus;
import com.pet.operation.entity.ContentReview;
import com.pet.operation.mapper.ContentReviewMapper;
import com.pet.operation.service.ContentReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ContentReviewServiceImpl implements ContentReviewService {

    private final ContentReviewMapper contentReviewMapper;

    /**
     * 【内容审核服务实现的构造注入】
     *
     * 业务作用：注入内容审核记录 Mapper。
     *
     * 调用场景：Spring 容器创建 ContentReviewServiceImpl 时。
     *
     * 调用链：Spring 容器 → 本构造方法 → 赋值 contentReviewMapper。
     *
     * 数据处理：仅保存依赖引用。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：无。
     */
    public ContentReviewServiceImpl(ContentReviewMapper contentReviewMapper) {
        this.contentReviewMapper = contentReviewMapper;
    }

    /**
     * 查询所有待审核的举报记录
     */
    @Override
    public List<ContentReview> listPending() {
        log.info("listPending() 被调用");
        return contentReviewMapper.selectList(
                new LambdaQueryWrapper<ContentReview>()
                        .eq(ContentReview::getStatus_wsh, ReviewStatus.PENDING)
                        .orderByDesc(ContentReview::getCreated_at_wsh));
    }

    /**
     * 查询全部内容审核记录
     */
    @Override
    public List<ContentReview> listAll() {
        log.info("listAll() 被调用");
        return contentReviewMapper.selectList(
                new LambdaQueryWrapper<ContentReview>().orderByDesc(ContentReview::getCreated_at_wsh));
    }

    /**
     * 分页查询待审核的举报记录
     */
    @Override
    public IPage<ContentReview> listPendingPage(PageRequestDTO pageParam) {
        log.info("listPendingPage() 被调用");
        Page<ContentReview> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return contentReviewMapper.selectPage(page,
                new LambdaQueryWrapper<ContentReview>()
                        .eq(ContentReview::getStatus_wsh, ReviewStatus.PENDING)
                        .orderByDesc(ContentReview::getCreated_at_wsh));
    }

    /**
     * 分页查询全部内容审核记录
     */
    @Override
    public IPage<ContentReview> listPage(PageRequestDTO pageParam) {
        log.info("listPage() 被调用");
        Page<ContentReview> page = new Page<>(pageParam.getPage(), pageParam.getSize());
        return contentReviewMapper.selectPage(page,
                new LambdaQueryWrapper<ContentReview>().orderByDesc(ContentReview::getCreated_at_wsh));
    }

    /**
     * 用户举报内容，创建一条 PENDING 状态的审核记录
     */
    @Override
    @Transactional
    public ContentReview report(String targetType, Long targetId, Long reporterId, String reason) {
        log.info("report() 被调用");
        ContentReview cr = new ContentReview();
        cr.setTarget_type_wsh(targetType);
        cr.setTarget_id_wsh(targetId);
        cr.setReporter_id_wsh(reporterId);
        cr.setReason_wsh(reason);
        cr.setStatus_wsh(ReviewStatus.PENDING); // 举报初始状态固定为待审核
        contentReviewMapper.insert(cr);
        return cr;
    }

    /**
     * 审核通过，将状态置为 APPROVED
     */
    @Override
    @Transactional
    public ContentReview approve(Long id, Long reviewerId, String remark) {
        log.info("approve() 被调用");
        ContentReview cr = getById(id);
        cr.setStatus_wsh(ReviewStatus.APPROVED);
        cr.setReviewer_id_wsh(reviewerId);
        cr.setReview_remark_wsh(remark);
        contentReviewMapper.updateById(cr);
        return cr;
    }

    /**
     * 驳回审核，将状态置为 REJECTED
     */
    @Override
    @Transactional
    public ContentReview reject(Long id, Long reviewerId, String remark) {
        log.info("reject() 被调用");
        ContentReview cr = getById(id);
        cr.setStatus_wsh(ReviewStatus.REJECTED);
        cr.setReviewer_id_wsh(reviewerId);
        cr.setReview_remark_wsh(remark);
        contentReviewMapper.updateById(cr);
        return cr;
    }

    /**
     * 【加载审核记录并校验存在】
     *
     * 业务作用：按主键加载内容审核记录，不存在则抛异常。
     *
     * 调用场景：approve() 与 reject() 审核前加载记录。
     *
     * 调用链：approve()/reject() → getById() → contentReviewMapper.selectById()。
     *
     * 数据处理：按主键查询 content_review 单条记录。
     *
     * 业务规则：记录必须存在。
     *
     * 状态影响：无。
     *
     * 异常情况：记录为空抛 BusinessException("审核记录不存在")。
     *
     * 注意事项：未校验记录当前状态，审核与驳回可对任意状态记录执行。
     */
    private ContentReview getById(Long id) {
        ContentReview cr = contentReviewMapper.selectById(id);
        // 记录不存在直接中断审核
        if (cr == null) throw new BusinessException("审核记录不存在");
        return cr;
    }
}
