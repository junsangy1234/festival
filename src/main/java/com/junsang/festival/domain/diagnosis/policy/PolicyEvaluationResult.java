package com.junsang.festival.domain.diagnosis.policy;

import com.junsang.festival.domain.diagnosis.dto.response.RecommendationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;

import java.util.List;

// 정책 엔진이 계산 결과에서 도출한 리스크와 운영 제안을 함께 전달한다.
public record PolicyEvaluationResult(
        List<RiskResponse> risks,
        List<RecommendationResponse> recommendations
) {
}
