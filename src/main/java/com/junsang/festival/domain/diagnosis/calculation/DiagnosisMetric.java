package com.junsang.festival.domain.diagnosis.calculation;

import java.math.BigDecimal;
import java.util.Map;

// 계산기가 정책 엔진에 전달하는 하나의 수치 지표와 화면 표시용 부가 정보다.
public record DiagnosisMetric(
        String code,
        BigDecimal value,
        Map<String, String> attributes
) {
}
