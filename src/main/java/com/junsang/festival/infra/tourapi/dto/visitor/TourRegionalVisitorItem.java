package com.junsang.festival.infra.tourapi.dto.visitor;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TourRegionalVisitorItem(
        String signguCode,
        String signguName,
        String dayOfWeekCode,
        String dayOfWeekName,
        String visitorTypeCode,
        String visitorTypeName,
        BigDecimal visitorCount,
        LocalDate baseDate
) {
}
