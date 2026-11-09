package com.pet.common;

import java.util.Map;
import java.util.Set;

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
 * confirmed（已接单，等待看护者接收宠物）
 *   ↓ 看护者输入主人提供的 4 位交接码
 * received（已接收）
 *   ↓ 看护者上传开始服务照片
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
 * - 主人不需要手动标记"已送达"，接收由看护者凭交接码确认
 */
public final class OrderStatus {
    /**
     * 【订单状态常量类私有构造器】
     *
     * 业务作用：阻止实例化，保证该类只作为状态常量容器使用。
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
     * 注意事项：新增状态需同步状态机流转与统计口径。
     */
    private OrderStatus() {}
    /** 待付款 */
    public static final String PENDING = "pending";
    /** 已付款 */
    public static final String PAID = "paid";
    /** 已接单（商家/看护者确认接收） */
    public static final String CONFIRMED = "confirmed";
    /** 已接收（看护者凭交接码确认收到宠物） */
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

    /**
     * 管理员手动修正允许的状态转移。
     *
     * 业务规则：
     * 1. 只允许沿履约链向前推进一步，不允许跳步——跳过 received 会绕过交接码校验，
     *    跳过 in_progress 会绕过开始照片；
     * 2. 不允许倒退；
     * 3. 不允许触碰资金态（paid/completed/cancelled/refunding/refunded），
     *    这些必须走支付、取消、完成、退款专用接口；
     * 4. in_progress 没有后继：完成订单必须走 /orders/complete。
     */
    public static final Map<String, Set<String>> MANUAL_TRANSITIONS = Map.of(
            CONFIRMED, Set.of(RECEIVED),
            RECEIVED, Set.of(IN_PROGRESS)
    );
}
