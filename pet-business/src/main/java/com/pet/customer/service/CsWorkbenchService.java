package com.pet.customer.service;

import com.pet.customer.dto.CsConversationDTO;
import com.pet.customer.dto.CsMerchantDTO;
import com.pet.customer.dto.CsThreadDTO;
import com.pet.customer.dto.CsWorkbenchStatsDTO;

import java.util.List;

/**
 * 【业务模块】客服工作台
 * 业务作用：为客服角色提供工作台概览：处理量统计、服务商家列表、会话列表。
 * 权限规则：商家维度隔离——客服仅能统计/查看其授权服务商家的数据。
 */
public interface CsWorkbenchService {

    /**
     * 工作台统计
     *
     * 业务作用：汇总当前用户(客服/管理员/商家)在工作台看到的关键数量。
     */
    CsWorkbenchStatsDTO stats(Long staffUserId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 我服务的商家列表
     *
     * 业务作用：列出当前客服授权服务的商家及其待办数量。
     */
    List<CsMerchantDTO> merchants(Long staffUserId);

    /**
     * 会话列表
     *
     * 业务作用：列出当前用户的聊天会话摘要（对端、最近消息、未读数）。
     */
    List<CsConversationDTO> conversations(Long userId);

    /**
     * 业务线程列表（投诉/工单维度的独立会话）
     *
     * 业务作用：把投诉与工单按业务对象拆分为独立会话线程，同一用户的多张投诉/工单互不混淆。
     */
    List<CsThreadDTO> threads(Long userId, boolean admin, boolean merchant, boolean customerService);

    /**
     * 标记业务线程已读
     *
     * 业务作用：把指定投诉/工单线程中发给当前用户的消息全部标记为已读（红点消除）。
     */
    void markThreadRead(Long userId, String type, Long bizId);
}
