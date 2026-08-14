package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RiskResponse(
        String riskCode,
        String severity,
        Integer priority,
        String title,
        String description,
        String metricKey,
        BigDecimal metricValue,
        Map<String, String> evidence,
        List<String> recommendationCodes
) {
}
