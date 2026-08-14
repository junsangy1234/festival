package com.junsang.festival.domain.diagnosis.risk;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;

import java.util.List;

// 4축·5축 계산 결과를 바탕으로 리스크 경고를 생성하기 위한 인터페이스다.
public interface RiskGenerator {

    List<RiskResponse> generate(DiagnosisDataContext context);
}
