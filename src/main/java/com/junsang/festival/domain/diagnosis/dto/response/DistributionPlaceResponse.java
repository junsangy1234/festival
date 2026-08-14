package com.junsang.festival.domain.diagnosis.dto.response;

public record DistributionPlaceResponse(
        Integer rank,
        String placeName,
        String recommendationReason,
        Double value
) {
}
