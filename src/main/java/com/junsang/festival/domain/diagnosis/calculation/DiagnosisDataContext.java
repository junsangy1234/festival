package com.junsang.festival.domain.diagnosis.calculation;

import com.junsang.festival.domain.diagnosis.entity.Diagnosis;

import java.util.Map;
import java.util.List;

// 외부 API 원본 데이터를 진단 계산기로 전달하기 위한 내부 컨텍스트다.
public record DiagnosisDataContext(
        Diagnosis diagnosis,
        Map<String, Object> sourceData,
        List<DiagnosisMetric> metrics
) {
    // 계산 결과가 아직 없는 단계에서 정책 엔진을 호출할 수 있는 빈 컨텍스트를 만든다.
    public static DiagnosisDataContext empty(Diagnosis diagnosis) {
        return new DiagnosisDataContext(diagnosis, Map.of(), List.of());
    }
}
