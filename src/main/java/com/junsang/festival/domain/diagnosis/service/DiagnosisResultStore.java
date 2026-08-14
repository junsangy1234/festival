package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// 진단 정제 결과 메모리 보관
@Component
public class DiagnosisResultStore {

    private final ConcurrentMap<String, DiagnosisDataContext> contexts = new ConcurrentHashMap<>();

    // 정제 결과 저장
    public void put(String reportId, DiagnosisDataContext context) {
        contexts.put(reportId, context);
    }

    // 정제 결과 조회
    public Optional<DiagnosisDataContext> get(String reportId) {
        return Optional.ofNullable(contexts.get(reportId));
    }
}
