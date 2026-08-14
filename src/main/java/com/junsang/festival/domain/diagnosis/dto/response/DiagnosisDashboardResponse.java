package com.junsang.festival.domain.diagnosis.dto.response;

import com.junsang.festival.domain.diagnosis.DiagnosisStatus;

import java.util.List;

public record DiagnosisDashboardResponse(
        String reportId,
        DiagnosisStatus status,
        DiagnosisResponse diagnosis,
        TargetLocationResponse targetLocation,
        List<ExternalDataStatusResponse> dataStatuses,
        DataProfileResponse profile,
        ConcentrationResponse concentration,
        VolatilityResponse volatility,
        DistributionResponse distribution,
        RegionalVisitorResponse regionalVisitors,
        RelatedPlacesResponse relatedPlaces,
        CompetingResponse competing,
        List<RiskResponse> risks,
        List<RecommendationResponse> recommendations
) {
    public static DiagnosisDashboardResponse pending(DiagnosisResponse diagnosis) {
        return new DiagnosisDashboardResponse(
                diagnosis.reportId(),
                diagnosis.status(),
                diagnosis,
                null,
                List.of(),
                new DataProfileResponse(null, null, null, null),
                new ConcentrationResponse(List.of()),
                new VolatilityResponse(null, 0, List.of()),
                new DistributionResponse(null, 0, List.of()),
                new RegionalVisitorResponse(null, null, List.of(), null, null, null, null),
                new RelatedPlacesResponse(0, List.of(), null, 0),
                new CompetingResponse(0, 0, List.of(), 0),
                List.of(),
                List.of()
        );
    }
}
