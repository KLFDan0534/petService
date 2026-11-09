package com.pet.common;

/**
 * 【退款状态常量定义】
 *
 * 业务作用：
 * 定义退款申请的生命周期状态。
 * 退款由用户发起，经商家/管理员审核后完成退款。
 *
 * 状态流转：
 * pending（退款申请中）
 *   ├→ approved（审核通过）→ completed（退款完成，资金已退回）
 *   └→ rejected（审核拒绝）
 *
 * 注意事项：
 * - 退款完成后，订单状态变更为 refunded
 * - 退款流程涉及钱包余额变动，需要事务一致性保证
 */
public final class RefundStatus {
    /**
     * 【退款状态常量类私有构造器】
     *
     * 业务作用：阻止实例化，保证该类只作为退款状态常量容器使用。
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
     * 注意事项：状态值需与订单 refunding / refunded 流转保持一致。
     */
    private RefundStatus() {}
    /** 退款申请中 */
    public static final String PENDING = "pending";
    /** 审核通过（等待财务处理） */
    public static final String APPROVED = "approved";
    /** 退款完成（资金已退回） */
    public static final String COMPLETED = "completed";
    /** 审核拒绝 */
    public static final String REJECTED = "rejected";
}
