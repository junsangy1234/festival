package com.junsang.festival.domain.festival.dto.response;

import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalDetailInfoItem;

public record FestivalInformationResponse(String name, String content) {
    // 관광공사 상세 안내 항목을 우리 안내 응답으로 변환한다.
    public static FestivalInformationResponse from(TourFestivalDetailInfoItem item) {
        return new FestivalInformationResponse(item.infoname(), item.infotext());
    }
}
