package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompetitionFestivalResponse(
        String contentId,
        String festivalName,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String summary,
        BigDecimal distanceKm
) {
}
