package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RelatedPlacesResponse(
        Integer totalBasePlaceCount,
        List<BasePlaceResponse> basePlaces,
        BigDecimal averageTopTenRank,
        Integer categoryDiversity
) {
    public record BasePlaceResponse(
            String placeCode,
            String placeName,
            Integer totalRelatedPlaceCount,
            List<RelatedPlaceResponse> relatedPlaces
    ) {
    }

    public record RelatedPlaceResponse(
            String placeCode,
            String placeName,
            String regionName,
            String signguName,
            String largeCategory,
            String mediumCategory,
            String smallCategory,
            Integer rank
    ) {
    }
}
