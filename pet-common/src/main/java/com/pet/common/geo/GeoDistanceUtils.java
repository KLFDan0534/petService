package com.pet.common.geo;

import java.math.BigDecimal;

/**
 * 【地理坐标距离计算工具】
 *
 * 业务作用：
 * 基于 Haversine 公式计算两个 GPS 坐标点之间的球面距离。
 * 用于商家/看护者位置搜索中按距离排序和筛选。
 *
 * 调用场景：
 * 用户搜索附近商家时，计算用户位置与商家位置的距离。
 *
 * 调用链：
 * MerchantService.searchNearby()
 *   ↓
 * GeoDistanceUtils.distanceMeters()
 *
 * 数据处理：
 * - 输入：两个坐标点的经纬度（支持 BigDecimal 和 double 两种入参）
 * - 输出：两点之间的球面距离，单位为米
 * - 算法：Haversine 公式，地球半径 6371km
 *
 * 注意事项：
 * - 适用于短距离和中距离计算，精度约 0.5%
 * - 不适合极地地区的距离计算
 * - 计算前建议先用 isValidCoordinate 校验坐标合法性
 */
public final class GeoDistanceUtils {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeoDistanceUtils() {
    }

    /**
     * 计算两个 BigDecimal 经纬度坐标之间的距离（米）
     * 调用方：Service 层坐标查询场景
     */
    public static double distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        return distanceMeters(lat1.doubleValue(), lng1.doubleValue(), lat2.doubleValue(), lng2.doubleValue());
    }

    /**
     * 【Haversine 公式核心计算】
     * 计算两个 double 类型经纬度坐标之间的球面距离。
     *
     * 算法说明：
     * 1. 将经纬度转为弧度
     * 2. 计算纬度差和经度差
     * 3. 应用 Haversine 公式：a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
     * 4. c = 2 · atan2(√a, √(1-a))
     * 5. 距离 = R · c（R=6371km）
     *
     * 注意：此方法假设输入坐标已经过合法性校验。
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 【坐标合法性校验】
     * 验证经纬度是否在有效范围内。
     * 纬度范围：-90 ~ 90
     * 经度范围：-180 ~ 180
     *
     * 调用方：保存商家/地址坐标前进行数据清洗
     *
     * 返回 false 的场景：
     * - latitude 或 longitude 为 null
     * - 纬度不在 [-90, 90] 范围内
     * - 经度不在 [-180, 180] 范围内
     */
    public static boolean isValidCoordinate(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}
