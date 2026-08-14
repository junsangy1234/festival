package com.junsang.festival.domain.diagnosis.recommendation;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.dto.response.RecommendationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;

import java.util.List;

// 발생한 리스크와 진단 데이터를 운영 제안으로 변환하기 위한 인터페이스다.
public interface RecommendationGenerator {

    List<RecommendationResponse> generate(DiagnosisDataContext context, List<RiskResponse> risks);
}
