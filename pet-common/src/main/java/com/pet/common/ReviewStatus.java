package com.pet.common;

/**
 * 【审核状态常量定义】
 *
 * 业务作用：
 * 定义实名认证、商家入驻、看护者认证等审核流程的统一状态常量。
 * 由多个模块共用（User、Merchant、Keeper、Qualification 等）。
 *
 * 状态流转：
 * pending（待审核）
 *   ├→ approved（审核通过，允许执行业务）
 *   └→ rejected（审核驳回，需重新提交）
 *
 * 使用场景：
 * - User.real_name_status：实名认证审核
 * - Merchant.status：商家入驻审核
 * - Keeper.status：看护者认证审核
 * - Qualification.status：资质审核
 */
public final class ReviewStatus {
    /**
     * 【审核状态常量类私有构造器】
     *
     * 业务作用：阻止实例化，保证该类只作为审核状态常量容器使用。
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
     * 注意事项：商家、看护者、实名审核共用同一套状态取值。
     */
    private ReviewStatus() {}
    /** 待审核 */
    public static final String PENDING = "pending";
    /** 审核通过 */
    public static final String APPROVED = "approved";
    /** 审核驳回 */
    public static final String REJECTED = "rejected";
}
