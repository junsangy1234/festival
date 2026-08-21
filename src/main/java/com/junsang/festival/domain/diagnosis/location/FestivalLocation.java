package com.junsang.festival.domain.diagnosis.location;

import java.math.BigDecimal;

// 기획서 Part 5.5 — 축제장 위치 처리 결과다.
// precise=false면 시군구 중심 근사 좌표이므로 R-VOL-005·O-INF-003 판정을 건너뛴다.
public record FestivalLocation(
        BigDecimal latitude,
        BigDecimal longitude,
        LocationSource source,
        boolean precise,
        String address,
        String notice
) {
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public static FestivalLocation unavailable() {
        return new FestivalLocation(
                null, null, LocationSource.UNAVAILABLE, false, null,
                "축제장 좌표를 확보하지 못해 위치 기반 판정(R-VOL-005·O-INF-003)을 건너뜁니다."
        );
    }
}
