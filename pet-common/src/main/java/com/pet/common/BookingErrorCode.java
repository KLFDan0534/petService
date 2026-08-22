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

    /** 服务计费单位未知/不支持（既不是 day/session/hour 的任一规范化形式） */
    public static final String UNSUPPORTED_SERVICE_UNIT = "UNSUPPORTED_SERVICE_UNIT";

    /** 请求提交的计费单位与客户端单位不一致/与订单语义冲突 */
    public static final String UNIT_MISMATCH = "UNIT_MISMATCH";

    /** 请求提交的计费数量不合法（session 必须为 1、hour 必须大于 0 且不超过上限等） */
    public static final String QUANTITY_INVALID = "QUANTITY_INVALID";

    /** 服务时长不合法（非 15..1440 分钟、按小时非整小时等） */
    public static final String DURATION_INVALID = "DURATION_INVALID";

    /** 选择的槽位不可用（已被占用/不在营业时段/时长覆盖不完整） */
    public static final String SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE";

    /** 服务价格不合法（负数、超上限或超过两位小数精度） */
    public static final String PRICE_INVALID = "PRICE_INVALID";

    /** 服务分类不存在 */
    public static final String CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";

    /** 服务分类已禁用 */
    public static final String CATEGORY_DISABLED = "CATEGORY_DISABLED";

    /** 服务状态不合法（仅允许启用/禁用） */
    public static final String INVALID_STATUS = "INVALID_STATUS";

    /** 服务/商家/分类路径参数不是正数 */
    public static final String INVALID_PRODUCT_ID = "INVALID_PRODUCT_ID";

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

    /** 产品图册超过最大数量 */
    public static final String MEDIA_LIMIT_EXCEEDED = "MEDIA_LIMIT_EXCEEDED";

    /** 同一产品重复引用同一文件 */
    public static final String MEDIA_DUPLICATE_FILE = "MEDIA_DUPLICATE_FILE";

    /** 产品图片排序非法（缺失/重复/非连续） */
    public static final String MEDIA_INVALID_ORDER = "MEDIA_INVALID_ORDER";

    /** 产品封面非法（非空图册必须恰好一张封面） */
    public static final String MEDIA_INVALID_COVER = "MEDIA_INVALID_COVER";

    /** 引用的文件记录不存在 */
    public static final String MEDIA_FILE_NOT_FOUND = "MEDIA_FILE_NOT_FOUND";

    /** 文件不是产品用途（非 product-purpose 上传） */
    public static final String MEDIA_FILE_PURPOSE_MISMATCH = "MEDIA_FILE_PURPOSE_MISMATCH";

    /** 文件归属商家与产品商家不一致 */
    public static final String MEDIA_FILE_MERCHANT_MISMATCH = "MEDIA_FILE_MERCHANT_MISMATCH";

    /** 上传内容不是受支持的光栅图片格式（含 SVG/HTML 伪装） */
    public static final String IMAGE_FORMAT_UNSUPPORTED = "IMAGE_FORMAT_UNSUPPORTED";

    /** 图片解码失败（截断/损坏） */
    public static final String IMAGE_DECODE_FAILED = "IMAGE_DECODE_FAILED";

    /** 图片超过字节大小上限 */
    public static final String IMAGE_TOO_LARGE = "IMAGE_TOO_LARGE";

    /** 图片解码尺寸超过上限 */
    public static final String IMAGE_DIMENSIONS_EXCEEDED = "IMAGE_DIMENSIONS_EXCEEDED";

    /** 图片内容可疑（HTML/脚本/多语种文件等） */
    public static final String IMAGE_CONTENT_SUSPICIOUS = "IMAGE_CONTENT_SUSPICIOUS";

    /** 上传时文件记录落库失败，已补偿删除对象 */
    public static final String PRODUCT_IMAGE_RECORD_FAILED = "PRODUCT_IMAGE_RECORD_FAILED";
}