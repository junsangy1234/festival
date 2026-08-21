package com.junsang.festival.domain.diagnosis.dto.response;

public record VolatilityPlaceResponse(
        String placeName,
        Double increasePoint,
        String level,
        Double selfAverage,
        Double peakRate,
        java.time.LocalDate peakDate
) {
}
