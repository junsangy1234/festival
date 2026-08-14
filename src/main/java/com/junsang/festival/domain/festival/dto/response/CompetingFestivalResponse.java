package com.junsang.festival.domain.festival.dto.response;

import java.util.List;

public record CompetingFestivalResponse(
        String targetContentId,
        List<FestivalSummaryResponse> festivals,
        int count,
        int excludedMissingCoordinatesCount
) {
}
