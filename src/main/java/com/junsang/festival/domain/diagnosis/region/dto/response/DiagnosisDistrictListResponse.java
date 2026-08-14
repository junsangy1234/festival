package com.junsang.festival.domain.diagnosis.region.dto.response;

import java.util.List;

public record DiagnosisDistrictListResponse(
        String areaCode,
        String areaName,
        List<DiagnosisDistrictResponse> districts
) {
}
