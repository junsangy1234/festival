package com.junsang.festival.domain.diagnosis.risk;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;
import com.junsang.festival.domain.diagnosis.policy.RiskPolicyProperties;
import com.junsang.festival.domain.diagnosis.policy.PolicyTemplateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 계산 지표와 YAML 리스크 정책을 매칭해 리스크 경고를 생성한다.
@Component
@RequiredArgsConstructor
public class PolicyRiskGenerator implements RiskGenerator {

    private final RiskPolicyProperties policyProperties;
    private final PolicyTemplateResolver templateResolver;

    // 활성 규칙 중 조건을 만족한 리스크를 정책 우선순위 순으로 정렬한다.
    @Override
    public List<RiskResponse> generate(DiagnosisDataContext context) {
        return context.metrics().stream()
                .flatMap(metric -> safeRisks().stream()
                        .filter(RiskPolicyProperties.RiskRule::enabled)
                        .filter(rule -> rule.metricKey() != null && rule.metricKey().equals(metric.code()))
                        .filter(rule -> rule.operator() != null && rule.threshold() != null && metric.value() != null)
                        .filter(rule -> rule.operator().matches(metric.value(), rule.threshold()))
                        .map(rule -> new MatchedRisk(rule, metric)))
                .sorted(Comparator
                        .comparingInt((MatchedRisk matched) -> matched.rule().priority())
                        .thenComparing(matched -> matched.metric().value(), Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    // 설정이 비어 있는 개발 초기에도 정책 엔진이 안전하게 동작하도록 한다.
    private List<RiskPolicyProperties.RiskRule> safeRisks() {
        return Objects.requireNonNullElse(policyProperties.rules(), List.of());
    }

    // 정책 코드와 템플릿을 API 응답 형태로 변환한다.
    private RiskResponse toResponse(MatchedRisk matched) {
        RiskPolicyProperties.RiskRule rule = matched.rule();
        DiagnosisMetric metric = matched.metric();
        return new RiskResponse(
                rule.riskCode(),
                rule.severity(),
                rule.priority(),
                rule.title() == null ? rule.riskCode() : templateResolver.resolve(rule.title(), metric),
                templateResolver.resolve(rule.description(), metric),
                rule.metricKey(),
                metric.value(),
                evidence(rule, metric),
                Objects.requireNonNullElse(rule.recommendationCodes(), List.of())
        );
    }

    private Map<String, String> evidence(RiskPolicyProperties.RiskRule rule, DiagnosisMetric metric) {
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("metricValue", metric.value().stripTrailingZeros().toPlainString());
        List<String> evidenceFields = rule.evidenceFields() == null ? List.of() : rule.evidenceFields();
        for (String field : evidenceFields) {
            String value = metric.attributes().get(field);
            if (value != null) {
                evidence.put(field, value);
            }
        }
        return evidence;
    }

    private record MatchedRisk(RiskPolicyProperties.RiskRule rule, DiagnosisMetric metric) {
    }
}
