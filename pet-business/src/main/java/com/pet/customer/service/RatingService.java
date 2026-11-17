package com.pet.customer.service;

import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingDTO;

import java.util.List;

/**
 * 【业务模块】评价管理
 * 业务作用：提供用户对商家/照看者的评价功能，包括打分、评论内容管理和评价汇总。
 * 支持三种评价目标类型：merchant（商家）、keeper（照看者）、service（服务）。
 */
public interface RatingService {
    /**
     * 按目标查询评价列表
     *
     * 业务作用：根据目标对象 ID 和类型查询评价列表。
     *
     * @param targetId   目标对象 ID（商家ID/照看者ID/服务ID）
     * @param targetType 目标类型（merchant/keeper/service）
     * @return 评价DTO列表，按创建时间倒序
     */
    List<RatingDTO> getRatingsByTarget(Long targetId, String targetType);

    /**
     * 创建评价
     *
     * 业务作用：用户为指定订单或服务创建评价。
     * 业务规则：同一订单不可重复评价；订单需已完成。
     * 异常情况：目标为空抛异常；评分越界抛异常；订单未完成抛异常；重复评价抛异常。
     * 注意事项：服务维度评价自动查找最近已完成订单。
     *
     * @param userId  评价人用户 ID
     * @param request 评价创建请求
     * @return 创建成功的评价 DTO
     */
    RatingDTO createRating(Long userId, RatingCreateRequestDTO request);

    /**
     * 回复评价
     *
     * 业务作用：商家或照看者回复一条评价。
     * 业务规则：商家只能回复 merchant 类型评价；照看者只能回复 keeper 类型评价。
     * 异常情况：评价不存在抛异常；无权回复抛 BusinessException(403)。
     *
     * @param id     评价 ID
     * @param reply  回复内容
     * @param userId 回复者用户 ID
     * @return 更新后的评价 DTO
     */
    RatingDTO replyRating(Long id, String reply, Long userId);

    /**
     * 查询当前用户对指定订单的评价
     *
     * 业务作用：返回当前用户对某订单已提交的所有评价维度，用于前端展示已评价状态。
     * 业务规则：仅返回当前用户自己的评价。
     */
    List<RatingDTO> getMyRatingsByOrder(Long userId, Long orderId);
}
