package com.pet.common;

/**
 * 【全局统一状态码枚举】
 *
 * 业务作用：
 * 集中管理所有业务实体的状态整型值，避免各模块硬编码。
 * 不同模块同一整型值可能含义不同（如 0 在用户=禁用，在商户=待审核），
 * 因此不设计全局唯一状态值，而是按业务模块归类。
 *
 * 使用场景：
 * 实体 status 字段赋值、查询条件比较时引用，如：
 * {@code entity.setStatus(StatusCode.USER_ACTIVE.getValue())}
 *
 * 注意事项：
 * 新增状态值时应在此枚举中注册，并注明所属业务模块。
 */
public enum StatusCode {

    // ==================== 用户 (User) ====================
    /** 用户状态：正常/启用 */
    USER_ACTIVE(1),
    /** 用户状态：被封禁/禁用 */
    USER_BANNED(0),

    // ==================== 商家 (Merchant) ====================
    /** 商家状态：待审核（管理员审核入驻申请） */
    MERCHANT_PENDING(0),
    /** 商家状态：审核通过，正常营业中 */
    MERCHANT_APPROVED(1),
    /** 商家状态：审核未通过 */
    MERCHANT_REJECTED(2),

    // ==================== 看护员 (Keeper) ====================
    /** 看护员状态：待审核（商家/管理员审核入驻申请） */
    KEEPER_PENDING(0),
    /** 看护员状态：审核通过，可接单 */
    KEEPER_ACTIVE(1),
    /** 看护员状态：审核未通过 */
    KEEPER_REJECTED(2),
    /** 看护员状态：离线，不可接单 */
    KEEPER_OFFLINE(3),
    /** 看护员状态：忙碌，暂不接单 */
    KEEPER_BUSY(4),
    /** 看护员状态：已主动辞职 */
    KEEPER_RESIGNED(5),
    /** 看护员状态：被商家终止雇佣关系 */
    KEEPER_TERMINATED(6),

    // ==================== 服务项目 (ServiceItem) ====================
    /** 服务项目状态：已上架/启用，用户可见可下单 */
    SERVICE_ENABLED(1),
    /** 服务项目状态：已下架/禁用，用户不可见不可下单 */
    SERVICE_DISABLED(0),

    // ==================== 公告 (Notice) ====================
    /** 公告状态：已发布，对用户可见 */
    NOTICE_ACTIVE(1),
    /** 公告状态：草稿/未发布，仅管理员可见编辑 */
    NOTICE_DRAFT(0),

    // ==================== 操作日志 (OperationLog) ====================
    /** 操作日志状态：执行成功 */
    LOG_SUCCESS(1),
    /** 操作日志状态：执行失败 */
    LOG_FAILURE(0);

    private final int value;

    StatusCode(int value) {
        this.value = value;
    }

    /**
     * Returns the integer value of this status code.
     * Used for entity field assignment and query condition comparison.
     *
     * @return the integer status value
     */
    public int getValue() {
        return value;
    }
}
