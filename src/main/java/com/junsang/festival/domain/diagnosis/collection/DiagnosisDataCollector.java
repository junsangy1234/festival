package com.junsang.festival.domain.diagnosis.collection;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;

// 관광공사와 데이터 담당 산출물을 내부 진단 데이터로 수집·변환하기 위한 인터페이스다.
public interface DiagnosisDataCollector {

    DiagnosisDataContext collect(Diagnosis diagnosis);
}
