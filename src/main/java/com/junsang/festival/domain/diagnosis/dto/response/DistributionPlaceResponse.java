package com.junsang.festival.domain.diagnosis.dto.response;

// 뷰 03 여유 관광지 카드. 관광지명·유형·연관 순위·평균 집중률을 함께 표시한다.
public record DistributionPlaceResponse(
        Integer rank,
        String placeName,
        String category,
        Integer relatedRank,
        Integer hubRank,
        String recommendationReason,
        Double value
) {
}
