package com.junsang.festival.domain.diagnosis.dto.response;

// 특정 날짜의 관광지 집중률 원본 값과 자기 평균 대비 변화를 담는다.
public record ConcentrationSpotResponse(
        String placeName,
        Double concentrationRate,
        Double deltaFromAverage
) {
}
