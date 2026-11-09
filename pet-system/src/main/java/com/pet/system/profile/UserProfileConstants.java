package com.pet.system.profile;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 【用户资料常量定义】
 *
 * 业务作用：
 * 定义用户实名认证的全生命周期状态常量。
 * 实名认证是用户使用核心业务功能（下单、成为商家/看护者）的前置条件。
 *
 * 状态流转：
 * REAL_NAME_UNVERIFIED(0) — 未实名（初始状态）
 *   ↓ 用户提交认证信息
 * REAL_NAME_PENDING(1) — 待审核
 *   ├→ 管理员审核通过
 *   │  REAL_NAME_VERIFIED(2) — 已认证（可使用全部功能）
 *   └→ 管理员驳回
 *      REAL_NAME_REJECTED(3) — 驳回
 *        ↓ 用户修改信息后重新提交
 *        （回到 REAL_NAME_PENDING）
 *
 * 使用场景：
 * - User.real_name_status 字段
 * - UserProfileRequirementService.ensureAllowed() 校验
 * - RealNameReviewServiceImpl 审核操作
 *
 * 注意事项：
 * - 状态 0（未实名）与 null（从未提交）在业务上等价
 * - 状态 2（已认证）是使用高级功能的必要条件
 */
@Schema(description = "用户资料常量")
public final class UserProfileConstants {
    /** 未实名（初始状态，用户从未提交过认证信息） */
    public static final int REAL_NAME_UNVERIFIED = 0;
    /** 待审核（用户已提交认证信息，等待管理员审核） */
    public static final int REAL_NAME_PENDING = 1;
    /** 已认证（审核通过，用户可正常使用平台全部功能） */
    public static final int REAL_NAME_VERIFIED = 2;
    /** 已驳回（审核未通过，需用户修改资料后重新提交） */
    public static final int REAL_NAME_REJECTED = 3;

    /**
     * 【用户资料常量类私有构造器】
     *
     * 业务作用：阻止实例化，保证该类只作为常量容器使用。
     *
     * 调用场景：无；常量类不会被实例化。
     *
     * 调用链：无。
     *
     * 数据处理：无。
     *
     * 业务规则：常量类不允许创建实例。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：实名状态取值需与数据库存量值保持一致。
     */
    private UserProfileConstants() {
    }
}
