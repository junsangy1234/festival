package com.junsang.festival.domain.diagnosis.recommendation;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.dto.response.RecommendationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;
import com.junsang.festival.domain.diagnosis.policy.RecommendationPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

// 발생 리스크와 YAML 제안 정책을 매칭해 운영 제안을 생성한다.
@Component
@RequiredArgsConstructor
public class PolicyRecommendationGenerator implements RecommendationGenerator {

    private final RecommendationPolicyProperties policyProperties;

    // 리스크 등급, 제안 카테고리, 실행 난이도 순으로 운영 제안의 우선순위를 부여한다.
    @Override
    public List<RecommendationResponse> generate(DiagnosisDataContext context, List<RiskResponse> risks) {
        Set<String> riskCodes = risks.stream().map(RiskResponse::riskCode).collect(java.util.stream.Collectors.toSet());
        List<RecommendationPolicyProperties.RecommendationRule> matchedRules = safeRecommendations().stream()
                .filter(RecommendationPolicyProperties.RecommendationRule::enabled)
                .filter(rule -> Objects.requireNonNullElse(rule.relatedRiskCodes(), List.of()).stream().anyMatch(riskCodes::contains))
                .sorted(Comparator.comparingInt(RecommendationPolicyProperties.RecommendationRule::priority))
                .toList();

        return matchedRules.stream()
                .map(rule -> toResponse(rule, risks))
                .toList();
    }

    // 설정 문구와 연결 리스크의 근거 수치를 Claude 전달 전 단계 구조로 만든다.
    private RecommendationResponse toResponse(
            RecommendationPolicyProperties.RecommendationRule rule,
            List<RiskResponse> risks
    ) {
        List<RiskResponse> relatedRisks = risks.stream()
                .filter(risk -> Objects.requireNonNullElse(rule.relatedRiskCodes(), List.of()).contains(risk.riskCode()))
                .toList();
        Map<String, java.math.BigDecimal> evidenceValues = new LinkedHashMap<>();
        relatedRisks.forEach(risk -> evidenceValues.put(risk.metricKey(), risk.metricValue()));
        return new RecommendationResponse(
                rule.recommendationCode(),
                rule.priority(),
                rule.category(),
                rule.difficulty(),
                rule.title(),
                rule.defaultAction(),
                relatedRisks.stream().map(RiskResponse::riskCode).toList(),
                evidenceValues
        );
    }

    // 설정이 비어 있는 개발 초기에도 정책 엔진이 안전하게 동작하도록 한다.
    private List<RecommendationPolicyProperties.RecommendationRule> safeRecommendations() {
        return Objects.requireNonNullElse(policyProperties.recommendations(), List.of());
    }
}
