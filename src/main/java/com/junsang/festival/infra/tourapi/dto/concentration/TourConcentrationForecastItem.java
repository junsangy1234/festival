package com.junsang.festival.infra.tourapi.dto.concentration;

import java.math.BigDecimal;
import java.time.LocalDate;

// 관광공사 집중률 예측 응답 한 건을 서비스에서 사용할 타입으로 정규화한 값이다.
public record TourConcentrationForecastItem(
        LocalDate baseDate,
        String areaCode,
        String areaName,
        String signguCode,
        String signguName,
        String placeName,
        BigDecimal concentrationRate
) {
}
