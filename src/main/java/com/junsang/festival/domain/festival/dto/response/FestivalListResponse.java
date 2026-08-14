package com.junsang.festival.domain.festival.dto.response;

import java.util.List;

public record FestivalListResponse(
        List<FestivalSummaryResponse> festivals,
        int pageNo,
        int size,
        int totalCount
) {
}
