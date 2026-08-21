package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 뷰 04 동기간 인근 축제 카드. API #8 정보에 CSV #9 실측(방문객수·예산)을 조인한다.
public record CompetitionFestivalResponse(
        String contentId,
        String festivalName,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String summary,
        BigDecimal distanceKm,
        BigDecimal lastYearVisitors,
        BigDecimal budgetMillionWon,
        List<String> linkageTags
) {
}
