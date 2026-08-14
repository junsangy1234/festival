package com.junsang.festival.domain.diagnosis.analysis;

import java.math.BigDecimal;
import java.util.List;

public record RelatedPlaceAnalysisResult(
        List<BasePlaceRelations> basePlaces,
        BigDecimal averageTopTenRank,
        int categoryDiversity
) {
    public record BasePlaceRelations(
            String placeCode,
            String placeName,
            List<RelatedPlace> relatedPlaces
    ) {
    }

    public record RelatedPlace(
            String placeCode,
            String placeName,
            String regionName,
            String signguName,
            String largeCategory,
            String mediumCategory,
            String smallCategory,
            int rank
    ) {
    }
}
