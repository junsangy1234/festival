package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;

// 기획서 5.5.2·5.5.3 — 축제장 좌표와 그 출처·정밀도·안내 문구를 담는다.
public record FestivalLocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String source,
        boolean precise,
        String address,
        String notice
) {
}
