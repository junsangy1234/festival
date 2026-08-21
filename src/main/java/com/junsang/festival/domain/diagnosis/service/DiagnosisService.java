package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.collection.DiagnosisDataCollector;
import com.junsang.festival.domain.diagnosis.data.ExternalDataResult;
import com.junsang.festival.domain.diagnosis.data.ExternalDataStatus;
import com.junsang.festival.domain.diagnosis.dto.request.DiagnosisRequest;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisDashboardResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisResponse;
import com.junsang.festival.domain.diagnosis.dto.response.ForecastReportResponse;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.diagnosis.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// 진단 생성·상태·대시보드 처리
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisDataCollector diagnosisDataCollector;
    private final DiagnosisResultStore diagnosisResultStore;
    private final DiagnosisDashboardAssembler dashboardAssembler;
    private final ForecastReportAssembler forecastReportAssembler;

    // 진단 생성과 데이터 수집
    public DiagnosisResponse createDiagnosis(DiagnosisRequest request) {
        Diagnosis diagnosis = Diagnosis.create(request);
        diagnosisRepository.save(diagnosis);
        collectAndStore(diagnosis);
        return DiagnosisResponse.from(diagnosis);
    }

    // 진단 상태 조회
    public DiagnosisResponse getDiagnosis(String reportId) {
        return DiagnosisResponse.from(findDiagnosis(reportId));
    }

    // M2 대시보드 조립
    public DiagnosisDashboardResponse getDashboard(String reportId) {
        Diagnosis diagnosis = findDiagnosis(reportId);
        DiagnosisDataContext context = diagnosisResultStore.get(reportId)
                .orElseGet(() -> collectAndStore(diagnosis));
        return dashboardAssembler.assemble(diagnosis, context);
    }

    // M3 예보 리포트 조립
    public ForecastReportResponse getForecastReport(String reportId) {
        Diagnosis diagnosis = findDiagnosis(reportId);
        DiagnosisDataContext context = diagnosisResultStore.get(reportId)
                .orElseGet(() -> collectAndStore(diagnosis));
        DiagnosisDashboardResponse dashboard = dashboardAssembler.assemble(diagnosis, context);
        return forecastReportAssembler.assemble(diagnosis, dashboard);
    }

    // 진단 조회
    private Diagnosis findDiagnosis(String reportId) {
        return diagnosisRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다: " + reportId));
    }

    // 외부 데이터 수집·저장
    private DiagnosisDataContext collectAndStore(Diagnosis diagnosis) {
        diagnosis.markRunning();
        diagnosisRepository.save(diagnosis);
        try {
            DiagnosisDataContext context = diagnosisDataCollector.collect(diagnosis);
            diagnosisResultStore.put(diagnosis.getId(), context);
            updateStatus(diagnosis, context);
            diagnosisRepository.save(diagnosis);
            return context;
        } catch (RuntimeException exception) {
            diagnosis.markFailed();
            diagnosisRepository.save(diagnosis);
            DiagnosisDataContext empty = DiagnosisDataContext.empty(diagnosis);
            diagnosisResultStore.put(diagnosis.getId(), empty);
            return empty;
        }
    }

    // 전체 처리 상태 결정
    private void updateStatus(Diagnosis diagnosis, DiagnosisDataContext context) {
        var results = context.sourceData().values().stream()
                .filter(ExternalDataResult.class::isInstance)
                .map(ExternalDataResult.class::cast)
                .toList();
        boolean allAvailable = !results.isEmpty()
                && results.stream().allMatch(result -> result.status() == ExternalDataStatus.AVAILABLE);
        boolean allFailed = !results.isEmpty()
                && results.stream().allMatch(result -> result.status() == ExternalDataStatus.FAILED);
        if (allAvailable) {
            diagnosis.markCompleted();
        } else if (allFailed) {
            diagnosis.markFailed();
        } else {
            diagnosis.markPartial();
        }
    }
}
