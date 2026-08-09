package com.pet.customer.service;

import com.pet.customer.dto.RatingCreateRequestDTO;
import com.pet.customer.dto.RatingDTO;
import com.pet.customer.entity.Rating;

import java.util.List;

/**
 * 【业务模块】评价管理
 * 业务作用：提供用户对商家/照看者的评价功能，包括打分、评论内容管理和评价汇总。
 * 支持三种评价目标类型：merchant（商家）、keeper（照看者）、service（服务）。
 */
public interface RatingService {
    /**
     * 【业务名称】按目标查询评价列表
     * 业务作用：根据目标对象 ID 和类型查询评价列表。
     * 调用场景：用户或商家查看某目标的评价。
     * 调用链：getRatingsByTarget() → RatingMapper.selectList()。
     * 数据处理：按 targetId + targetType 匹配，按创建时间倒序。
     * 业务规则：无。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：无。
     *
     * @param targetId   目标对象 ID（商家ID/照看者ID/服务ID）
     * @param targetType 目标类型（merchant/keeper/service）
     * @return 评价DTO列表，按创建时间倒序
     */
    List<RatingDTO> getRatingsByTarget(Long targetId, String targetType);

    /**
     * 【业务名称】创建评价
     * 业务作用：用户为指定订单或服务创建评价。
     * 调用场景：订单完成后用户评价。
     * 调用链：createRating() → 校验 → RatingMapper.insert()。
     * 数据处理：校验订单完成状态和重复评价 → 插入评价记录。
     * 业务规则：同一订单不可重复评价；订单需已完成。
     * 状态影响：新增评价记录。
     * 异常情况：目标为空抛异常；评分越界抛异常；订单未完成抛异常；重复评价抛异常。
     * 注意事项：服务维度评价自动查找最近已完成订单。
     *
     * @param userId  评价人用户 ID
     * @param request 评价创建请求
     * @return 创建成功的评价 DTO
     */
    RatingDTO createRating(Long userId, RatingCreateRequestDTO request);

    /**
     * 【业务名称】回复评价
     * 业务作用：商家或照看者回复一条评价。
     * 调用场景：商家/照看者回复用户评价。
     * 调用链：replyRating() → 权限校验 → RatingMapper.updateById()。
     * 数据处理：更新评价的 reply 和 reply_at 字段。
     * 业务规则：商家只能回复 merchant 类型评价；照看者只能回复 keeper 类型评价。
     * 状态影响：评价的 reply 字段更新。
     * 异常情况：评价不存在抛异常；无权回复抛 BusinessException(403)。
     * 注意事项：无。
     *
     * @param id     评价 ID
     * @param reply  回复内容
     * @param userId 回复者用户 ID
     * @return 更新后的评价 DTO
     */
    RatingDTO replyRating(Long id, String reply, Long userId);
}
