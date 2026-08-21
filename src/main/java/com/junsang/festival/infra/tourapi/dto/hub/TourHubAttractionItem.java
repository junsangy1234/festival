package com.junsang.festival.infra.tourapi.dto.hub;

import java.math.BigDecimal;

// API #6 기초지자체중심 관광지정보의 한 항목이다. 좌표(mapX·mapY)와 hubRank를 함께 담는다.
public record TourHubAttractionItem(
        String baseYearMonth,
        String areaCode,
        String signguCode,
        String placeCode,
        String placeName,
        String largeCategory,
        String mediumCategory,
        String smallCategory,
        Integer hubRank,
        BigDecimal longitude,
        BigDecimal latitude
) {
    // 좌표가 모두 있는 항목만 지도·거리 계산에 사용한다.
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
