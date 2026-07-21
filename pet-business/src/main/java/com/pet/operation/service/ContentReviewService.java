package com.pet.operation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.common.PageRequestDTO;
import com.pet.operation.entity.ContentReview;

import java.util.List;

public interface ContentReviewService {
    /**
     * 获取待审核的内容列表
     * @return 待审核内容列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ContentReview> listPending();
    /**
     * 获取所有内容审核记录
     * @return 内容审核列表
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    List<ContentReview> listAll();
    /**
     * 分页查询待审核内容
     * @param pageParam 分页参数
     * @return 分页待审核数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<ContentReview> listPendingPage(PageRequestDTO pageParam);
    /**
     * 分页查询所有内容审核记录
     * @param pageParam 分页参数
     * @return 分页审核数据
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    IPage<ContentReview> listPage(PageRequestDTO pageParam);
    /**
     * 举报内容
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param reporterId 举报人ID
     * @param reason 举报原因
     * @return 创建的内容审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ContentReview report(String targetType, Long targetId, Long reporterId, String reason);
    /**
     * 审核通过内容
     * @param id 审核ID
     * @param reviewerId 审核人ID
     * @param remark 审核备注
     * @return 更新后的审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ContentReview approve(Long id, Long reviewerId, String remark);
    /**
     * 驳回内容审核
     * @param id 审核ID
     * @param reviewerId 审核人ID
     * @param remark 驳回原因
     * @return 更新后的审核记录
     * @author: wsh
     * @date: 2026/6/24 11:05
     **/
    ContentReview reject(Long id, Long reviewerId, String remark);
}

