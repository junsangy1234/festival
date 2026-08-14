package com.junsang.festival.domain.diagnosis.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConcentrationAnalysisResult(
        BigDecimal festivalPeriodAverage,
        List<DailyConcentration> dailyConcentrations,
        List<VolatilityPlace> volatilityPlaces,
        List<RelaxedPlaceCandidate> relaxedPlaceCandidates
) {
    public record DailyConcentration(
            LocalDate date,
            List<PlaceRate> places
    ) {
    }

    public record PlaceRate(
            String placeName,
            BigDecimal concentrationRate,
            BigDecimal deltaFromThirtyDayAverage
    ) {
    }

    public record VolatilityPlace(
            String placeName,
            BigDecimal thirtyDayAverage,
            BigDecimal peakRate,
            BigDecimal peakDelta,
            LocalDate peakDate,
            ConcentrationBadge badge
    ) {
    }

    public record RelaxedPlaceCandidate(
            String placeName,
            BigDecimal thirtyDayAverage,
            BigDecimal festivalPeriodAverage,
            BigDecimal deltaFromThirtyDayAverage
    ) {
    }
}
