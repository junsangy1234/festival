package com.junsang.festival.domain.diagnosis.dto.response;

// 서로 합산하지 않는 데이터 프로필 막대 하나의 원본 값과 표시 범위를 담는다.
public record DataProfileMetricResponse(
        Number value,
        Number minimum,
        Number maximum,
        String unit,
        String direction
) {
}
