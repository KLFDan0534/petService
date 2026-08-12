package com.pet.common;

/**
 * 【预约业务稳定错误标识】
 *
 * 业务作用：
 * 为预约流程（创建、支付确认、看护员容量等）提供稳定的机器可读错误标识，
 * 前端按此标识映射为可理解提示，不解析易变的中文 message。
 *
 * 约定：
 * - 标识字符串一经发布不得随意变更，新增时在此枚举登记。
 * - 与 code 配合使用：code 决定 HTTP 状态，errorCode 决定提示映射。
 */
public final class BookingErrorCode {

    private BookingErrorCode() {
    }

    /** 商家未开放未来预约（future_booking_enabled_wsh != 1） */
    public static final String FUTURE_BOOKING_DISABLED = "FUTURE_BOOKING_DISABLED";

    /** 送达/接回时间不在目标日期营业时段内 */
    public static final String FULFILLMENT_OUTSIDE_BUSINESS_HOURS = "FULFILLMENT_OUTSIDE_BUSINESS_HOURS";

    /** 看护者容量不足或与既有排班冲突 */
    public static final String CAPACITY_EXCEEDED = "CAPACITY_EXCEEDED";

    /** 看护者当前不可服务（离职、终止、未审核等） */
    public static final String KEEPER_NOT_BOOKABLE = "KEEPER_NOT_BOOKABLE";

    /** 商家未通过审核 */
    public static final String MERCHANT_NOT_APPROVED = "MERCHANT_NOT_APPROVED";

    /** 服务不存在 */
    public static final String SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND";

    /** 服务已下架 */
    public static final String SERVICE_OFF_SHELF = "SERVICE_OFF_SHELF";

    /** 客户端提交的商家与服务归属不一致 */
    public static final String SERVICE_MERCHANT_MISMATCH = "SERVICE_MERCHANT_MISMATCH";

    /** 服务计费单位不是 day，本期不支持 */
    public static final String UNSUPPORTED_SERVICE_UNIT = "UNSUPPORTED_SERVICE_UNIT";

    /** 目标日期为商家休息日 */
    public static final String MERCHANT_REST_DAY = "MERCHANT_REST_DAY";

    /** 看护员不属于服务所属商家 */
    public static final String KEEPER_MERCHANT_MISMATCH = "KEEPER_MERCHANT_MISMATCH";

    /** 看护员资质未通过 */
    public static final String KEEPER_NOT_QUALIFIED = "KEEPER_NOT_QUALIFIED";

    /** 看护员请假 */
    public static final String KEEPER_ON_LEAVE = "KEEPER_ON_LEAVE";

    /** 宠物档期冲突 */
    public static final String PET_BOOKING_CONFLICT = "PET_BOOKING_CONFLICT";

    /** 服务价格或版本已变化，需要前端刷新确认 */
    public static final String PRICE_CHANGED = "PRICE_CHANGED";

    /** 订单未完成，不能评价 */
    public static final String ORDER_NOT_COMPLETED = "ORDER_NOT_COMPLETED";

    /** 评价目标与订单归属不一致 */
    public static final String RATING_TARGET_MISMATCH = "RATING_TARGET_MISMATCH";

    /** 同一订单同一维度评价已存在 */
    public static final String RATING_ALREADY_EXISTS = "RATING_ALREADY_EXISTS";

    /** 服务列表排序参数非法 */
    public static final String INVALID_SORT_PARAM = "INVALID_SORT_PARAM";

    /** 可用性查询日期范围非法 */
    public static final String AVAILABILITY_RANGE_INVALID = "AVAILABILITY_RANGE_INVALID";

    /** 可预约查询参数非法（keeper 不属于服务商家等） */
    public static final String INVALID_AVAILABILITY_PARAM = "INVALID_AVAILABILITY_PARAM";
}