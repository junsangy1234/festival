package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;

public record TargetLocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String source
) {
}
