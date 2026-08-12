package com.pet.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 【服务版本工具】
 *
 * 业务作用：
 * 服务版本是"服务驱动下单"链路中的一致性凭证：详情页/可预约性响应与订单创建请求
 * 必须使用同一版本格式，服务更新后版本变化，客户端以旧版本下单会被拒绝（PRICE_CHANGED）。
 * 本类作为版本的唯一格式化来源，避免各模块各自实现导致格式漂移。
 */
public final class ServiceVersions {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ServiceVersions() {
    }

    /**
     * 将服务更新时间格式化为版本字符串（ISO 本地日期时间，如 2026-08-11T13:45:20）。
     *
     * @param updatedAt 服务更新时间
     * @return 版本字符串；updatedAt 为 null 时返回 null
     */
    public static String format(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return null;
        }
        return updatedAt.format(FORMATTER);
    }
}
