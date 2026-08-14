package com.junsang.festival.domain.festival.dto.response;

import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalImageItem;

public record FestivalImageResponse(String imageUrl, String thumbnailUrl, String name) {
    // 관광공사 이미지 항목을 우리 이미지 응답으로 변환한다.
    public static FestivalImageResponse from(TourFestivalImageItem item) {
        return new FestivalImageResponse(item.imageUrl(), item.thumbnailUrl(), item.imgname());
    }
}
