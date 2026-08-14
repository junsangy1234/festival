package com.junsang.festival.domain.diagnosis.dto.response;

// M2 상단에서 독립적으로 표시하는 4개 데이터 프로필을 담는다.
public record DataProfileResponse(
        DataProfileMetricResponse timingFit,
        DataProfileMetricResponse relaxedPlaces,
        DataProfileMetricResponse connectivity,
        DataProfileMetricResponse categoryDiversity
) {
}
