package com.pet.common.geo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceUtilsTest {

    @Test
    void distanceMetersReturnsSmallDistanceForNearbyPoints() {
        double distance = GeoDistanceUtils.distanceMeters(
                new BigDecimal("31.2304000"),
                new BigDecimal("121.4737000"),
                new BigDecimal("31.2305000"),
                new BigDecimal("121.4738000"));

        assertThat(distance).isGreaterThan(0).isLessThan(20);
    }

    @Test
    void isValidCoordinateRejectsMissingOrOutOfRangeValues() {
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), new BigDecimal("121.4737"))).isTrue();
        assertThat(GeoDistanceUtils.isValidCoordinate(null, new BigDecimal("121.4737"))).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("91"), new BigDecimal("121.4737"))).isFalse();
        assertThat(GeoDistanceUtils.isValidCoordinate(new BigDecimal("31.2304"), new BigDecimal("181"))).isFalse();
    }
}
