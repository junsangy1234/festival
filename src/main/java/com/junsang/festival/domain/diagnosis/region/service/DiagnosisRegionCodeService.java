package com.junsang.festival.domain.diagnosis.region.service;

import com.junsang.festival.domain.diagnosis.region.DiagnosisRegionCode;
import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisDistrictListResponse;
import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisDistrictResponse;
import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisRegionListResponse;
import com.junsang.festival.domain.diagnosis.region.dto.response.DiagnosisRegionResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 진단용 시도·시군구 코드표
@Service
public class DiagnosisRegionCodeService {

    private static final String CODE_RESOURCE = "diagnosis-region-codes.csv";

    private Map<String, Area> areasByCode = Map.of();

    // 코드표 CSV 로드
    @PostConstruct
    void initialize() {
        Map<String, Area> areas = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CODE_RESOURCE).getInputStream(), StandardCharsets.UTF_8
        ))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);
                if (columns.length != 4) {
                    continue;
                }
                DiagnosisRegionCode code = new DiagnosisRegionCode(columns[0], columns[1], columns[2], columns[3]);
                areas.computeIfAbsent(code.areaCode(), key -> new Area(code.areaCode(), code.areaName(), new LinkedHashMap<>()))
                        .districts().put(code.signguCode(), code.signguName());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("진단 지역 코드표를 읽지 못했습니다.", exception);
        }
        areasByCode = Map.copyOf(areas);
    }

    // 시도 목록 조회
    public DiagnosisRegionListResponse getRegions() {
        List<DiagnosisRegionResponse> regions = areasByCode.values().stream()
                .sorted(Comparator.comparing(Area::areaCode))
                .map(area -> new DiagnosisRegionResponse(area.areaCode(), area.areaName()))
                .toList();
        return new DiagnosisRegionListResponse(regions);
    }

    // 시군구 목록 조회
    public DiagnosisDistrictListResponse getDistricts(String areaCode) {
        Area area = areasByCode.get(areaCode);
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "지원하지 않는 진단 지역 코드입니다: " + areaCode);
        }
        List<DiagnosisDistrictResponse> districts = area.districts().entrySet().stream()
                .map(entry -> new DiagnosisDistrictResponse(entry.getKey(), entry.getValue()))
                .toList();
        return new DiagnosisDistrictListResponse(area.areaCode(), area.areaName(), districts);
    }

    private record Area(String areaCode, String areaName, Map<String, String> districts) {
    }
}
