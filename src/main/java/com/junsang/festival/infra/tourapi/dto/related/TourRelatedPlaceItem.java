package com.junsang.festival.infra.tourapi.dto.related;

public record TourRelatedPlaceItem(
        String baseYearMonth,
        String basePlaceCode,
        String basePlaceName,
        String areaCode,
        String areaName,
        String signguCode,
        String signguName,
        String relatedPlaceCode,
        String relatedPlaceName,
        String relatedAreaCode,
        String relatedAreaName,
        String relatedSignguCode,
        String relatedSignguName,
        String largeCategory,
        String mediumCategory,
        String smallCategory,
        int rank
) {
}
