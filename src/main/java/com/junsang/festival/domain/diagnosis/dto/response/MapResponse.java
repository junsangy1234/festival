package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 기획서 Part 5.5 · 축제장 위치 기반 지도 표현용 응답이다.
// 좌표가 근사치(시군구 중심)면 precise=false이며 notice를 지도에 함께 노출한다.
public record MapResponse(
        SiteMarkerResponse site,
        Double nearestRadiusKm,
        List<PlaceMarkerResponse> places,
        List<FestivalMarkerResponse> nearbyFestivals,
        String notice
) {
    // 축제장 마커
    public record SiteMarkerResponse(
            BigDecimal latitude,
            BigDecimal longitude,
            String source,
            boolean precise,
            String address
    ) {
    }

    // 관광지 마커. 좌표는 API #6, 집중률·상승폭은 API #1에서 온다.
    public record PlaceMarkerResponse(
            String placeName,
            BigDecimal latitude,
            BigDecimal longitude,
            String category,
            Integer hubRank,
            String badge,
            Double peakIncreasePoint,
            BigDecimal distanceKm,
            boolean withinNearestRadius
    ) {
    }

    // 동기간 인근 축제 마커
    public record FestivalMarkerResponse(
            String contentId,
            String festivalName,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal distanceKm,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
