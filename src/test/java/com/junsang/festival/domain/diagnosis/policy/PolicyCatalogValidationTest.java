package com.junsang.festival.domain.diagnosis.policy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 배포되는 정책 YAML을 그대로 바인딩해 오타·누락·깨진 상호참조를 부팅 전에 잡는다.
class PolicyCatalogValidationTest {

    private static RiskPolicyProperties riskPolicy;
    private static RecommendationPolicyProperties catalog;

    @BeforeAll
    static void loadPolicies() throws Exception {
        riskPolicy = bind("policies/risk-rules.yml", "diagnosis.risk-policy", RiskPolicyProperties.class);
        catalog = bind(
                "policies/recommendation-catalog.yml", "diagnosis.recommendation-catalog",
                RecommendationPolicyProperties.class
        );
    }

    private static <T> T bind(String resource, String prefix, Class<T> type) throws Exception {
        var propertySources = new MutablePropertySources();
        new YamlPropertySourceLoader().load(resource, new ClassPathResource(resource))
                .forEach(propertySources::addLast);
        return new Binder(ConfigurationPropertySources.from(propertySources)).bind(prefix, type).get();
    }

    @Test
    void riskRulesHaveRequiredFields() {
        assertThat(riskPolicy.rules()).isNotEmpty();
        for (var rule : riskPolicy.rules()) {
            assertThat(rule.riskCode()).as("risk-code").isNotBlank();
            assertThat(rule.title()).as("%s title", rule.riskCode()).isNotBlank();
            assertThat(rule.severity()).as("%s severity", rule.riskCode()).isNotBlank();
            assertThat(rule.metricKey()).as("%s metric-key", rule.riskCode()).isNotBlank();
            assertThat(rule.operator()).as("%s operator", rule.riskCode()).isNotNull();
            assertThat(rule.threshold()).as("%s threshold", rule.riskCode()).isNotNull();
        }
    }

    @Test
    void recommendationsHaveRequiredFields() {
        assertThat(catalog.recommendations()).isNotEmpty();
        for (var recommendation : catalog.recommendations()) {
            assertThat(recommendation.recommendationCode()).as("recommendation-code").isNotBlank();
            assertThat(recommendation.title())
                    .as("%s title", recommendation.recommendationCode()).isNotBlank();
            assertThat(recommendation.defaultAction())
                    .as("%s default-action", recommendation.recommendationCode()).isNotBlank();
        }
    }

    @Test
    void codesAreUnique() {
        List<String> riskCodes = riskPolicy.rules().stream()
                .map(RiskPolicyProperties.RiskRule::riskCode).toList();
        List<String> recommendationCodes = catalog.recommendations().stream()
                .map(RecommendationPolicyProperties.RecommendationRule::recommendationCode).toList();
        assertThat(riskCodes).doesNotHaveDuplicates();
        assertThat(recommendationCodes).doesNotHaveDuplicates();
    }

    @Test
    void crossReferencesResolve() {
        Set<String> riskCodes = riskPolicy.rules().stream()
                .map(RiskPolicyProperties.RiskRule::riskCode).collect(Collectors.toSet());
        Set<String> recommendationCodes = catalog.recommendations().stream()
                .map(RecommendationPolicyProperties.RecommendationRule::recommendationCode)
                .collect(Collectors.toSet());

        for (var rule : riskPolicy.rules()) {
            assertThat(recommendationCodes)
                    .as("%s recommendation-codes", rule.riskCode())
                    .containsAll(rule.recommendationCodes());
        }
        for (var recommendation : catalog.recommendations()) {
            assertThat(riskCodes)
                    .as("%s related-risk-codes", recommendation.recommendationCode())
                    .containsAll(recommendation.relatedRiskCodes());
        }
    }
}
