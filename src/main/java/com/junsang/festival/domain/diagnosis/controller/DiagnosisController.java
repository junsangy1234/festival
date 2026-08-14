package com.junsang.festival.domain.diagnosis.controller;

import com.junsang.festival.domain.diagnosis.dto.request.DiagnosisRequest;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisDashboardResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisResponse;
import com.junsang.festival.domain.diagnosis.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// 축제 진단·M2 대시보드 API
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    // 진단 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse createDiagnosis(@Valid @RequestBody DiagnosisRequest request) {
        return diagnosisService.createDiagnosis(request);
    }

    // 진단 상태 조회
    @GetMapping("/{reportId}")
    public DiagnosisResponse getDiagnosis(@PathVariable String reportId) {
        return diagnosisService.getDiagnosis(reportId);
    }

    // M2 대시보드 조회
    @GetMapping("/{reportId}/dashboard")
    public DiagnosisDashboardResponse getDashboard(@PathVariable String reportId) {
        return diagnosisService.getDashboard(reportId);
    }

}
