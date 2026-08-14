package com.junsang.festival.domain.diagnosis.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegionalVisitorAnalysisResult(
        LocalDate referenceFestivalStartDate,
        LocalDate referenceFestivalEndDate,
        List<DailyVisitor> dailyVisitors,
        BigDecimal festivalPeriodAverage,
        BigDecimal beforePeriodAverage,
        BigDecimal afterPeriodAverage,
        BigDecimal changeFromBeforePercent
) {
    public record DailyVisitor(
            LocalDate date,
            BigDecimal localVisitors,
            BigDecimal outsideVisitors,
            BigDecimal foreignVisitors,
            BigDecimal totalVisitors
    ) {
    }
}
