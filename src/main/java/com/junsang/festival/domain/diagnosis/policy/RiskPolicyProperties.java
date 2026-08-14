package com.junsang.festival.domain.diagnosis.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

// 수정이 잦은 리스크 기준을 YAML에서 읽는다.
@ConfigurationProperties(prefix = "diagnosis.risk-policy")
public record RiskPolicyProperties(
        List<RiskRule> rules
) {
    public record RiskRule(
            String riskCode,
            String title,
            String description,
            String severity,
            int priority,
            String metricKey,
            ComparisonOperator operator,
            BigDecimal threshold,
            List<String> evidenceFields,
            List<String> recommendationCodes,
            boolean enabled
    ) {
    }
}
