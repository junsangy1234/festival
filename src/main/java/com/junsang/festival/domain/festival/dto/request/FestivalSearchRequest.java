package com.junsang.festival.domain.festival.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public record FestivalSearchRequest(
        @NotNull @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @NotNull @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate,
        String areaCode,
        String sigunguCode,
        String legalDongRegionCode,
        String legalDongSignguCode,
        @Min(1) Integer pageNo,
        @Min(1) Integer size
) {
    private static final DateTimeFormatter TOUR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    // 우리 검색 조건을 관광공사 searchFestival2 요청 파라미터로 변환한다.
    public Map<String, String> toTourParameters() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("eventStartDate", startDate.format(TOUR_DATE_FORMATTER));
        parameters.put("eventEndDate", endDate.format(TOUR_DATE_FORMATTER));
        parameters.put("pageNo", String.valueOf(pageNo == null ? 1 : pageNo));
        parameters.put("numOfRows", String.valueOf(size == null ? 20 : size));
        if (areaCode != null && !areaCode.isBlank()) {
            parameters.put("areaCode", areaCode);
        }
        if (sigunguCode != null && !sigunguCode.isBlank()) {
            parameters.put("sigunguCode", sigunguCode);
        }
        if (legalDongRegionCode != null && !legalDongRegionCode.isBlank()) {
            parameters.put("lDongRegnCd", legalDongRegionCode);
        }
        if (legalDongSignguCode != null && !legalDongSignguCode.isBlank()) {
            parameters.put("lDongSignguCd", legalDongSignguCode);
        }
        return parameters;
    }
}
