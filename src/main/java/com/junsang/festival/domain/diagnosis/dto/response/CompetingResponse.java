package com.junsang.festival.domain.diagnosis.dto.response;

import java.util.List;

// 동기간 경합 축제(View 04)의 화면 표시 정보를 담는다.
public record CompetingResponse(
        Integer totalCount,
        Integer displayedCount,
        List<CompetitionFestivalResponse> festivals,
        Integer excludedMissingCoordinatesCount
) {
}
