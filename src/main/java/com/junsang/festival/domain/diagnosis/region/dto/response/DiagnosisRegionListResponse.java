package com.junsang.festival.domain.diagnosis.region.dto.response;

import java.util.List;

public record DiagnosisRegionListResponse(
        List<DiagnosisRegionResponse> regions
) {
}
