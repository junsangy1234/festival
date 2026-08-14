package com.junsang.festival.domain.diagnosis.region.controller;

import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisDistrictListResponse;
import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisRegionListResponse;
import com.junsang.festival.domain.diagnosis.region.service.DiagnosisRegionCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 진단용 지역 코드 API
@RestController
@RequestMapping("/api/v1/diagnosis-regions")
@RequiredArgsConstructor
public class DiagnosisRegionCodeController {

    private final DiagnosisRegionCodeService diagnosisRegionCodeService;

    // 시도 목록 조회
    @GetMapping
    public DiagnosisRegionListResponse getRegions() {
        return diagnosisRegionCodeService.getRegions();
    }

    // 시군구 목록 조회
    @GetMapping("/{areaCode}/districts")
    public DiagnosisDistrictListResponse getDistricts(@PathVariable String areaCode) {
        return diagnosisRegionCodeService.getDistricts(areaCode);
    }
}
