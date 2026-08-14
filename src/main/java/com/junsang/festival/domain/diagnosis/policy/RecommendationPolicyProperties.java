package com.junsang.festival.domain.diagnosis.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// 리스크와 연결되는 운영 제안 카탈로그를 YAML에서 읽는다.
@ConfigurationProperties(prefix = "diagnosis.recommendation-catalog")
public record RecommendationPolicyProperties(
        List<RecommendationRule> recommendations
) {
    public record RecommendationRule(
            String recommendationCode,
            int priority,
            String title,
            String defaultAction,
            List<String> relatedRiskCodes,
            String category,
            String difficulty,
            boolean enabled
    ) {
    }
}
