package com.junsang.festival.domain.festivalhistory.dto;

import java.math.BigDecimal;

// CSV #9(문체부 지역축제 개최 계획 현황)의 축제 1건 실적이다.
public record FestivalHistoryRecord(
        String festivalName,
        String regionName,
        String signguName,
        BigDecimal lastYearVisitors,
        BigDecimal budgetMillionWon,
        Integer firstHeldYear,
        Integer roundCount
) {
}
