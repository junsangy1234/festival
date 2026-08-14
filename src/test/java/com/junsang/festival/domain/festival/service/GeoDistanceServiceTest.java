package com.junsang.festival.domain.festival.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDistanceServiceTest {

    private final GeoDistanceService service = new GeoDistanceService();

    @Test
    void includesDistanceAtFiftyKilometers() {
        double latitudeDelta = Math.toDegrees(50.0 / 6371.0088);

        boolean within = service.isWithinKm(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(latitudeDelta),
                BigDecimal.ZERO,
                50.0
        );

        assertThat(within).isTrue();
        assertThat(service.calculateKm(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(latitudeDelta),
                BigDecimal.ZERO
        )).isEqualByComparingTo("50.0");
    }

    @Test
    void excludesDistanceBeyondFiftyKilometers() {
        double latitudeDelta = Math.toDegrees(50.1 / 6371.0088);

        assertThat(service.isWithinKm(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(latitudeDelta),
                BigDecimal.ZERO,
                50.0
        )).isFalse();
    }
}
