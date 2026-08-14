package com.junsang.festival.domain.diagnosis.policy;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import com.junsang.festival.domain.diagnosis.recommendation.PolicyRecommendationGenerator;
import com.junsang.festival.domain.diagnosis.risk.PolicyRiskGenerator;
import com.junsang.festival.domain.diagnosis.service.DiagnosisPolicyEvaluationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisPolicyEvaluationServiceTest {

    @Test
    void appliesYamlPolicyToMetricsAndSortsRecommendations() {
        RiskPolicyProperties riskProperties = new RiskPolicyProperties(
                List.of(new RiskPolicyProperties.RiskRule(
                        "R-VOL-001", "{placeName} 급상승", "+{value}%p", "CRITICAL", 1,
                        "VOLATILITY_PEAK_INCREASE", ComparisonOperator.GREATER_THAN_OR_EQUAL,
                        BigDecimal.valueOf(25), List.of("date"), List.of("O-INF-001"), true
                ))
        );
        RecommendationPolicyProperties recommendationProperties = new RecommendationPolicyProperties(
                List.of(new RecommendationPolicyProperties.RecommendationRule(
                        "O-INF-001", 1, "셔틀 증차", "분산 안내", List.of("R-VOL-001"),
                        "인프라", "하", true
                ))
        );
        PolicyTemplateResolver templateResolver = new PolicyTemplateResolver();
        DiagnosisPolicyEvaluationService service = new DiagnosisPolicyEvaluationService(
                new PolicyRiskGenerator(riskProperties, templateResolver),
                new PolicyRecommendationGenerator(recommendationProperties)
        );
        DiagnosisDataContext context = new DiagnosisDataContext(
                null,
                Map.of(),
                List.of(new DiagnosisMetric(
                        "VOLATILITY_PEAK_INCREASE",
                        BigDecimal.valueOf(25),
                        Map.of("placeName", "구룡사", "date", "2026-08-20")
                ))
        );

        PolicyEvaluationResult result = service.evaluate(context);

        assertThat(result.risks()).singleElement()
                .extracting("riskCode", "title", "description")
                .containsExactly("R-VOL-001", "구룡사 급상승", "+25%p");
        assertThat(result.recommendations()).singleElement()
                .extracting("recommendationCode", "priority", "category", "difficulty")
                .containsExactly("O-INF-001", 1, "인프라", "하");
    }

    @Test
    void skipsDisabledRulesAndUnknownMetrics() {
        RiskPolicyProperties riskProperties = new RiskPolicyProperties(List.of(
                new RiskPolicyProperties.RiskRule(
                        "DISABLED", "", "", "WARNING", 1, "KNOWN",
                        ComparisonOperator.GREATER_THAN, BigDecimal.ZERO, List.of(), List.of(), false
                ),
                new RiskPolicyProperties.RiskRule(
                        "UNKNOWN", "", "", "WARNING", 2, "UNKNOWN",
                        ComparisonOperator.GREATER_THAN, BigDecimal.ZERO, List.of(), List.of(), true
                )
        ));
        DiagnosisPolicyEvaluationService service = new DiagnosisPolicyEvaluationService(
                new PolicyRiskGenerator(riskProperties, new PolicyTemplateResolver()),
                new PolicyRecommendationGenerator(new RecommendationPolicyProperties(List.of()))
        );
        DiagnosisDataContext context = new DiagnosisDataContext(
                null,
                Map.of(),
                List.of(new DiagnosisMetric("KNOWN", BigDecimal.TEN, Map.of()))
        );

        PolicyEvaluationResult result = service.evaluate(context);

        assertThat(result.risks()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
    }
}
