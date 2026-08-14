package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.policy.PolicyEvaluationResult;
import com.junsang.festival.domain.diagnosis.recommendation.RecommendationGenerator;
import com.junsang.festival.domain.diagnosis.risk.RiskGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 4축·5축 계산 후 리스크와 운영 제안을 순서대로 생성하는 정책 실행 진입점이다.
@Service
@RequiredArgsConstructor
public class DiagnosisPolicyEvaluationService {

    private final RiskGenerator riskGenerator;
    private final RecommendationGenerator recommendationGenerator;

    // 정책 평가
    public PolicyEvaluationResult evaluate(DiagnosisDataContext context) {
        var risks = riskGenerator.generate(context);
        var recommendations = recommendationGenerator.generate(context, risks);
        return new PolicyEvaluationResult(risks, recommendations);
    }
}
