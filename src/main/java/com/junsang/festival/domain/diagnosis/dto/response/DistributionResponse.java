package com.junsang.festival.domain.diagnosis.dto.response;

import java.util.List;

public record DistributionResponse(
        String summary,
        Integer totalCount,
        List<DistributionPlaceResponse> places
) {
}
