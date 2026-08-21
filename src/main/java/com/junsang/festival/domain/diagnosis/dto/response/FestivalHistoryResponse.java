package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;

// CSV #9 문체부 재개최 실적 카드(작년 방문객수·예산·회차)다.
public record FestivalHistoryResponse(
        String festivalName,
        String regionName,
        String signguName,
        BigDecimal lastYearVisitors,
        BigDecimal budgetMillionWon,
        Integer firstHeldYear,
        Integer roundCount
) {
}
