package com.junsang.festival.domain.festival.dto.response;

import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchItem;

import java.math.BigDecimal;

public record FestivalSummaryResponse(
        String contentId,
        String title,
        String eventStartDate,
        String eventEndDate,
        String areaCode,
        String sigunguCode,
        String legalDongRegionCode,
        String legalDongSignguCode,
        String address,
        String addressDetail,
        String longitude,
        String latitude,
        String firstImage,
        String thumbnailImage,
        String telephone,
        BigDecimal distanceKm
) {
    // 관광공사 축제 검색 항목을 우리 축제 요약 응답으로 변환한다.
    public static FestivalSummaryResponse from(TourFestivalSearchItem item) {
        return new FestivalSummaryResponse(
                item.contentId(), item.title(), item.eventStartDate(), item.eventEndDate(),
                item.areaCode(), item.sigunguCode(), item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.address(), item.addressDetail(),
                item.longitude(), item.latitude(), item.firstImage(), item.thumbnailImage(), item.tel(), null
        );
    }

    public static FestivalSummaryResponse from(TourFestivalSearchItem item, BigDecimal distanceKm) {
        return new FestivalSummaryResponse(
                item.contentId(), item.title(), item.eventStartDate(), item.eventEndDate(),
                item.areaCode(), item.sigunguCode(), item.legalDongRegionCode(), item.legalDongSignguCode(),
                item.address(), item.addressDetail(),
                item.longitude(), item.latitude(), item.firstImage(), item.thumbnailImage(), item.tel(), distanceKm
        );
    }
}
