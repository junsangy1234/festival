package com.junsang.festival.domain.diagnosis.dto.response;

import com.junsang.festival.domain.diagnosis.DiagnosisStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// M3 리포트(세로 스크롤 6개 섹션)와 A4 요약본 PDF 생성을 위한 응답이다.
// 기획서 원칙에 따라 종합 지수·순위·등급은 만들지 않는다.
public record ForecastReportResponse(
        String reportId,
        DiagnosisStatus status,
        // §1 히어로
        ReportHeroResponse hero,
        // §2 A4 요약본
        SummarySheetResponse summarySheet,
        // §3 데이터 요약
        DataSummaryResponse dataSummary,
        // §4 리스크
        List<RiskResponse> risks,
        // §5 운영 조정 제안
        OperationProposalResponse operationProposal,
        // §6 근거
        EvidenceResponse evidence
) {
    // §1 · 진단 시점(D-X)과 그 시점에 활용 가능한 데이터를 함께 표시한다(기획서 1.4).
    public record ReportHeroResponse(
            String festivalName,
            LocalDate startDate,
            LocalDate endDate,
            Integer daysUntilStart,
            String diagnosisTiming,
            boolean forecastDataActive,
            List<String> availableDataSources,
            String briefing
    ) {
    }

    // §2 · 결재·현장 첨부용 A4 1페이지 요약본
    public record SummarySheetResponse(
            String title,
            String festivalName,
            LocalDate startDate,
            LocalDate endDate,
            String areaCode,
            String signguCode,
            String festivalType,
            String scale,
            List<String> keyFacts,
            List<RiskResponse> topRisks,
            List<RecommendationResponse> topRecommendations,
            String dataSourceNote,
            String limitationNote
    ) {
    }

    // §3 · 4개 데이터 뷰와 수평 막대 4축 요약
    public record DataSummaryResponse(
            DataProfileResponse profile,
            ConcentrationResponse tourismFlow,
            VolatilityResponse volatility,
            DistributionResponse distribution,
            CompetingResponse nearbyFestivals,
            RegionalVisitorResponse regionalVisitors,
            FestivalHistoryResponse festivalHistory
    ) {
    }

    // §5 · 운영 조정 제안. AI 확장 결과는 ai-service가 이 항목을 받아 4단 구조로 확장한다.
    public record OperationProposalResponse(
            String guidanceNote,
            List<OperationChecklistItemResponse> items
    ) {
    }

    public record OperationChecklistItemResponse(
            String recommendationCode,
            Integer priority,
            String category,
            String difficulty,
            String title,
            String defaultAction,
            List<String> relatedRiskCodes,
            boolean checked
    ) {
    }

    // §6 · 근거와 데이터 한계
    public record EvidenceResponse(
            Instant generatedAt,
            List<ExternalDataStatusResponse> dataStatuses,
            List<String> referencePeriodNotes,
            FestivalLocationResponse festivalLocation,
            String limitationNote
    ) {
    }
}
