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
}