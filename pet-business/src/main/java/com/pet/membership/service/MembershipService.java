package com.pet.membership.service;

import com.pet.membership.dto.UserMembershipDTO;
import com.pet.membership.entity.UserMembership;

import java.util.List;

/**
 * 【业务模块】会员管理
 * 业务作用：提供用户会员状态查询、管理端列表查询和会员到期自动过期处理。
 * 会员与套餐（MemberPlan）关联，包含有效期、折扣率等权益。
 */
public interface MembershipService {
    /**
     * 【业务名称】获取当前会员信息
     * 业务作用：获取指定用户的当前会员信息。
     * 如果用户无会员记录，返回默认的 inactive 状态 DTO。
     * 如果会员已过期，自动刷新状态后再返回。
     * 调用场景：用户在个人中心查看会员状态。
     * 调用链：getCurrentMembership() → selectOne() → refreshIfExpired() → toDTO()。
     * 数据处理：查询用户会员记录 → 到期自动过期处理 → 关联套餐信息转DTO。
     * 业务规则：用户无会员时返回默认 inactive DTO（never null）；会员过期时自动刷新状态。
     * 状态影响：如果会员已过期，自动更新数据库状态为 expired。
     * 异常情况：用户ID为空抛 BusinessException(401)。
     * 注意事项：自动触发过期刷新，保证返回的会员状态实时准确。
     *
     * @param userId 用户 ID
     * @return 当前会员 DTO（never null）
     */
    UserMembershipDTO getCurrentMembership(Long userId);

    /**
     * 【业务名称】管理端查询会员列表
     * 业务作用：按状态筛选查询会员记录（管理员用）。
     * 查询前先自动执行过期处理。
     * 调用场景：后台会员管理。
     * 调用链：listMembershipsForAdmin() → expireMemberships() → selectList() → toDTO()。
     * 数据处理：过期处理 → 按状态筛选 → 按更新时间倒序。
     * 业务规则：支持 active/inactive/expired 筛选；查询前自动过期。
     * 状态影响：查询前触发批量过期处理。
     * 异常情况：不支持的状态值抛 BusinessException(400)。
     * 注意事项：无。
     *
     * @param status 可选筛选条件：active/inactive/expired，null 查全部
     * @return 会员 DTO 列表
     */
    List<UserMembershipDTO> listMembershipsForAdmin(String status);

    /**
     * 【业务名称】批量过期会员
     * 业务作用：将所有已过期的活跃会员标记为 expired。
     * 由定时任务定时调用。
     * 调用场景：定时调度任务。
     * 调用链：expireMemberships() → update()。
     * 数据处理：批量更新 expires_at < now 的 active 会员为 expired。
     * 业务规则：仅处理状态为 active、过期时间在当前的会员。
     * 状态影响：符合条件的会员状态从 active → expired。
     * 异常情况：无。
     * 注意事项：支持定时任务和内部调用。
     *
     * @return 过期的会员数量
     */
    int expireMemberships();

    /**
     * 【业务名称】会员实体转 DTO
     * 业务作用：将会员实体转换为 DTO，关联套餐名称、折扣率、快照信息。
     * 调用场景：内部转换。
     * 调用链：toDTO()。
     * 数据处理：字段拷贝 → 关联套餐信息 → 解析快照 → 计算剩余天数。
     * 业务规则：入参为 null 时返回 null；折扣率为空时默认为 1。
     * 状态影响：无。
     * 异常情况：无。
     * 注意事项：包含剩余天数和有效状态计算。
     *
     * @param membership 会员实体
     * @return 会员 DTO
     */
    UserMembershipDTO toDTO(UserMembership membership);
}
