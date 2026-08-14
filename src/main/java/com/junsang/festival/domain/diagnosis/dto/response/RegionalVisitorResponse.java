package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegionalVisitorResponse(
        LocalDate referenceFestivalStartDate,
        LocalDate referenceFestivalEndDate,
        List<DailyVisitorResponse> dailyVisitors,
        BigDecimal festivalPeriodAverage,
        BigDecimal beforePeriodAverage,
        BigDecimal afterPeriodAverage,
        BigDecimal changeFromBeforePercent
) {
    public record DailyVisitorResponse(
            LocalDate date,
            BigDecimal localVisitors,
            BigDecimal outsideVisitors,
            BigDecimal foreignVisitors,
            BigDecimal totalVisitors
    ) {
    }
}
