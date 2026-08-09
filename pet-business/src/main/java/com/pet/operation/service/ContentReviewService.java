package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.ContentReview;

import java.util.List;

/**
 * 内容审核服务接口，提供内容举报、审核（通过/驳回）及查询功能。
 * <p>
 * 用户可以对平台上的各类内容进行举报，由管理员进行审核处理。
 * 审核状态包括：待审核（PENDING）、已通过（APPROVED）、已驳回（REJECTED）。
 */
public interface ContentReviewService {
    /**
     * 获取所有待审核的举报记录，按创建时间倒序
     *
     * @return 待审核内容列表
     */
    List<ContentReview> listPending();

    /**
     * 获取所有内容审核记录（包含已处理），按创建时间倒序
     *
     * @return 全部审核记录列表
     */
    List<ContentReview> listAll();

    /**
     * 分页查询待审核的举报记录
     *
     * @param pageParam 分页参数（页码、每页大小）
     * @return 待审核记录分页数据
     */
    IPage<ContentReview> listPendingPage(PageRequestDTO pageParam);

    /**
     * 分页查询全部内容审核记录（包含已处理）
     *
     * @param pageParam 分页参数
     * @return 全部审核记录分页数据
     */
    IPage<ContentReview> listPage(PageRequestDTO pageParam);

    /**
     * 用户举报指定内容，创建一条状态为 PENDING 的审核记录
     *
     * @param targetType 被举报内容的目标类型
     * @param targetId   被举报内容的目标 ID
     * @param reporterId 举报人用户 ID
     * @param reason     举报原因
     * @return 创建完成的内容审核记录
     */
    ContentReview report(String targetType, Long targetId, Long reporterId, String reason);

    /**
     * 审核通过一条举报内容，将状态置为 APPROVED
     *
     * @param id         审核记录 ID
     * @param reviewerId 审核人用户 ID
     * @param remark     审核备注
     * @return 更新后的审核记录
     */
    ContentReview approve(Long id, Long reviewerId, String remark);

    /**
     * 驳回一条举报内容，将状态置为 REJECTED
     *
     * @param id         审核记录 ID
     * @param reviewerId 审核人用户 ID
     * @param remark     驳回原因
     * @return 更新后的审核记录
     */
    ContentReview reject(Long id, Long reviewerId, String remark);
}

