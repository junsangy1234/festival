package com.junsang.festival.domain.diagnosis.dto.response;

import java.time.LocalDate;
import java.util.List;

// 개최기간 관광 흐름(View 01)의 날짜별 집중률 데이터를 담는다.
public record ConcentrationResponse(
        List<DailyConcentrationResponse> dailyConcentrations
) {
    public record DailyConcentrationResponse(
            LocalDate date,
            Integer totalPlaceCount,
            List<ConcentrationSpotResponse> places
    ) {
    }
}
