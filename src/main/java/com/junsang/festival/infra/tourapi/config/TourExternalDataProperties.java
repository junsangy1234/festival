package com.junsang.festival.infra.tourapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@ConfigurationProperties(prefix = "festival.external-data")
public record TourExternalDataProperties(
        String relatedBaseYearMonth,
        @DefaultValue("5") int relatedBaseMonthsBack,
        int visitorReferenceYearsBack
) {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    // 명시 값이 있으면 그대로 쓰고, 없으면 현재에서 N개월 물러난 연월을 계산한다.
    // 관광데이터랩 집계가 몇 달 늦게 열리므로 N은 related-base-months-back으로 보정한다.
    public String resolvedRelatedBaseYearMonth() {
        if (relatedBaseYearMonth != null && !relatedBaseYearMonth.isBlank()) {
            return relatedBaseYearMonth;
        }
        return YearMonth.now().minusMonths(relatedBaseMonthsBack).format(YEAR_MONTH);
    }
}
