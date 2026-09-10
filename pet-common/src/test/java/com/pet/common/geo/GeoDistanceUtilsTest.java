package com.pet.common.geo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceUtilsTest {

    // ── 相同坐标 → 0 ──

    @Test
    void sameCoordinatesMetersReturnsZero() {
        double distance = GeoDistanceUtils.distanceMeters(31.23, 121.47, 31.23, 121.47);
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void sameCoordinatesKmReturnsZero() {
        double distance = GeoDistanceUtils.distanceKm(31.23, 121.47, 31.23, 121.47);
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void sameCoordinatesBigDecimalReturnsZero() {
        double distance = GeoDistanceUtils.distanceMeters(
                new BigDecimal("31.230000"), new BigDecimal("121.470000"),
                new BigDecimal("31.230000"), new BigDecimal("121.470000"));
        assertThat(distance).isEqualTo(0.0);
    }

    // ── 米和公里关系：distanceKm ≈ distanceMeters / 1000 ──

    @Test
    void kmAndMetersAreConsistent() {
        double meters = GeoDistanceUtils.distanceMeters(31.23, 121.47, 31.24, 121.48);
        double km = GeoDistanceUtils.distanceKm(31.23, 121.47, 31.24, 121.48);
        assertThat(km).isCloseTo(meters / 1000.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void kmAndMetersAreConsistentForLongDistance() {
        // 北京 → 上海 约 1068km
        double meters = GeoDistanceUtils.distanceMeters(39.9042, 116.4074, 31.2304, 121.4737);
        double km = GeoDistanceUtils.distanceKm(39.9042, 116.4074, 31.2304, 121.4737);
        assertThat(km).isCloseTo(meters / 1000.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(km).isBetween(1050.0, 1100.0);
    }

    // ── BigDecimal 与 double 入参结果一致 ──

    @Test
    void bigDecimalAndDoubleOverloadsProduceSameResult() {
        double fromDouble = GeoDistanceUtils.distanceMeters(31.23, 121.47, 31.24, 121.48);
        double fromBigDecimal = GeoDistanceUtils.distanceMeters(
                new BigDecimal("31.23"), new BigDecimal("121.47"),
                new BigDecimal("31.24"), new BigDecimal("121.48"));
        assertThat(fromBigDecimal).isCloseTo(fromDouble, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void kmBigDecimalAndDoubleOverloadsProduceSameResult() {
        double fromDouble = GeoDistanceUtils.distanceKm(31.23, 121.47, 31.24, 121.48);
        double fromBigDecimal = GeoDistanceUtils.distanceKm(
                new BigDecimal("31.23"), new BigDecimal("121.47"),
                new BigDecimal("31.24"), new BigDecimal("121.48"));
        assertThat(fromBigDecimal).isCloseTo(fromDouble, org.assertj.core.data.Offset.offset(0.0001));
    }

    // ── 正常距离（长沙两个坐标） ──

    @Test
    void distanceMetersReturnsReasonableValueForNearbyPoints() {
        // 长沙市内两点
        double distance = GeoDistanceUtils.distanceMeters(
                new BigDecimal("28.2282"), new BigDecimal("112.9388"),
                new BigDecimal("28.2300"), new BigDecimal("112.9400"));
        assertThat(distance).isGreaterThan(0).isLessThan(500);
    }

    @Test
    void distanceKmReturnsReasonableValueForNearbyPoints() {
        double km = GeoDistanceUtils.distanceKm(28.2282, 112.9388, 28.2300, 112.9400);
        assertThat(km).isGreaterThan(0.0).isLessThan(0.5);
    }

    // ── isValidCoordinate ──

    @Test
    void isValidCoordinateAcceptsValidValues() {
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), new BigDecimal("121.4737"))).isTrue();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("0"), new BigDecimal("0"))).isTrue();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("-90"), new BigDecimal("-180"))).isTrue();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("90"), new BigDecimal("180"))).isTrue();
    }

    @Test
    void isValidCoordinateRejectsNull() {
        assertThat(GeoDistanceUtils.isValidCoordinate(null, new BigDecimal("121.4737"))).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), null)).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(null, null)).isFalse();
    }

    @Test
    void isValidCoordinateRejectsOutOfRangeLatitude() {
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("91"), new BigDecimal("121.4737"))).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("-91"), new BigDecimal("121.4737"))).isFalse();
    }

    @Test
    void isValidCoordinateRejectsOutOfRangeLongitude() {
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), new BigDecimal("181"))).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), new BigDecimal("-181"))).isFalse();
    }

    // ── 边界值：赤道上 1 度经度 ≈ 111.32 km ──

    @Test
    void oneDegreeLongitudeOnEquator() {
        double km = GeoDistanceUtils.distanceKm(0, 0, 0, 1);
        assertThat(km).isBetween(110.0, 112.0);
    }

    // ── 纬度每 0.01 度 ≈ 1.11 km ──

    @Test
    void point01DegreeLatitude() {
        double km = GeoDistanceUtils.distanceKm(31.23, 121.47, 31.24, 121.47);
        assertThat(km).isBetween(1.0, 1.2);
    }
}
