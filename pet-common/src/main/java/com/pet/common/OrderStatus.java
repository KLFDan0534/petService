package com.pet.common;

/**
 * 【订单状态常量定义】
 *
 * 业务作用：
 * 定义寄养订单的完整生命周期状态机常量。
 * 所有模块统一引用此类的常量，避免硬编码字符串。
 *
 * 状态流转：
 *
 * pending（待付款）
 *   ↓ 用户付款
 * paid（已付款）
 *   ├→ confirmed（商家/看护者接单）
 *   └→ cancelled（商家拒单或超时取消）
 * confirmed（已接单）
 *   ↓ 主人送达宠物
 * delivered（已送达）
 *   ↓ 看护者确认接收（需交接码验证）
 * received（已接收）
 *   ↓ 看护者开始服务
 * in_progress（服务中）
 *   ↓ 看护者完成服务
 * completed（已完成）
 *
 * 退款分支（从 paid 及之后状态可发起）：
 * 任意状态 → refunding → refunded
 *
 * 注意事项：
 * - pending 状态的订单超时未支付将自动取消
 * - paid 状态且超时未接单将自动确认（autoAccept）
 * - 完成时触发 AI 报告生成和商家结算
 */
public final class OrderStatus {
    private OrderStatus() {}
    /** 待付款 */
    public static final String PENDING = "pending";
    /** 已付款 */
    public static final String PAID = "paid";
    /** 已接单（商家/看护者确认接收） */
    public static final String CONFIRMED = "confirmed";
    /** 已送达（主人已将宠物送到） */
    public static final String DELIVERED = "delivered";
    /** 已接收（看护者确认接收宠物） */
    public static final String RECEIVED = "received";
    /** 服务中 */
    public static final String IN_PROGRESS = "in_progress";
    /** 已完成 */
    public static final String COMPLETED = "completed";
    /** 已取消 */
    public static final String CANCELLED = "cancelled";
    /** 退款中 */
    public static final String REFUNDING = "refunding";
    /** 已退款 */
    public static final String REFUNDED = "refunded";
}
