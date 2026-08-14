package com.junsang.festival.domain.diagnosis.dto.response;

import java.util.List;

public record VolatilityResponse(
        String summary,
        Integer totalCount,
        List<VolatilityPlaceResponse> places
) {
}
