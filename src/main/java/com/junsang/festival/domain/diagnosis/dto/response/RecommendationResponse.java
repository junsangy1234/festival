package com.junsang.festival.domain.diagnosis.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RecommendationResponse(
        String recommendationCode,
        Integer priority,
        String category,
        String difficulty,
        String title,
        String defaultAction,
        List<String> relatedRiskCodes,
        Map<String, BigDecimal> evidenceValues
) {
}
