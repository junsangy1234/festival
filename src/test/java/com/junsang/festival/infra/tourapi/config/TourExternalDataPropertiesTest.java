package com.junsang.festival.infra.tourapi.config;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class TourExternalDataPropertiesTest {

    @Test
    void explicitBaseYearMonthWins() {
        var properties = new TourExternalDataProperties("202503", 5, 1);
        assertThat(properties.resolvedRelatedBaseYearMonth()).isEqualTo("202503");
    }

    @Test
    void blankBaseYearMonthFallsBackToMonthsBack() {
        var properties = new TourExternalDataProperties("", 5, 1);
        String expected = YearMonth.now().minusMonths(5).format(DateTimeFormatter.ofPattern("yyyyMM"));
        assertThat(properties.resolvedRelatedBaseYearMonth()).isEqualTo(expected);
    }
}
