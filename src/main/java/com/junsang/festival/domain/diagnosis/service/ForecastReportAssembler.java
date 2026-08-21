package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisDashboardResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisResponse;
import com.junsang.festival.domain.diagnosis.dto.response.FestivalHistoryResponse;
import com.junsang.festival.domain.diagnosis.dto.response.ForecastReportResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RecommendationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RiskResponse;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// M2 대시보드 결과를 M3 리포트(6개 섹션) 구조로 재구성한다.
// 종합 점수·등급은 만들지 않고 각 데이터 사실을 그대로 전달한다.
@Service
public class ForecastReportAssembler {

    // 기획서 8.4 · 필수 노출 문구
    private static final String LIMITATION_NOTE = "본 예측치는 KT 통신 기반 ML 추정치이며, "
            + "실제 방문자수와 차이가 있을 수 있습니다. 절대치가 아닌 상대 변동 파악용입니다.";
    private static final String DATA_SOURCE_NOTE = "한국관광공사 관광지 집중률 예측(API #1) · 연관 관광지(API #2) · "
            + "기초지자체중심 관광지정보(API #6) · 지역별 방문자수(API #7) · 국문 관광정보(API #8) · "
            + "문화체육관광부 지역축제 개최 계획 현황(CSV #9) 기준";
    // 기획서 6.2 방어 5 · 실무자 판단 우선 문구
    private static final String OPERATION_GUIDANCE = "AI 추정 방문 인원과 운영 조정 제안은 참고 정보입니다. "
            + "실제 배치 결정은 담당 부서·유관 기관 협의 후 진행을 권고합니다.";
    // 기획서 8.3 · 데이터별 시점 명시
    private static final List<String> REFERENCE_PERIOD_NOTES = List.of(
            "집중률 예측(API #1): 향후 26일 예측 (기준: 조회일)",
            "연관 관광지(API #2): 과거 1년 방문 패턴",
            "기초지자체중심 관광지(API #6): 기준 연월 스냅숏",
            "방문자수(API #7): 과거 연도 실적",
            "축제 이력(CSV #9): 전년 개최 실적"
    );
    // 예측 데이터가 활성화되는 진단 시점(기획서 1.4)
    private static final int FORECAST_ACTIVE_DAYS = 25;

    // M3 리포트 생성
    public ForecastReportResponse assemble(Diagnosis diagnosis, DiagnosisDashboardResponse dashboard) {
        DiagnosisResponse diagnosisResponse = dashboard.diagnosis();
        Integer daysUntilStart = daysUntil(diagnosisResponse.startDate());
        boolean forecastActive = daysUntilStart != null
                && daysUntilStart >= 0
                && daysUntilStart <= FORECAST_ACTIVE_DAYS;

        return new ForecastReportResponse(
                dashboard.reportId(),
                dashboard.status(),
                hero(diagnosisResponse, daysUntilStart, forecastActive),
                summarySheet(diagnosisResponse, dashboard),
                dataSummary(dashboard),
                dashboard.risks(),
                new ForecastReportResponse.OperationProposalResponse(
                        OPERATION_GUIDANCE, checklist(dashboard.recommendations())
                ),
                new ForecastReportResponse.EvidenceResponse(
                        Instant.now(),
                        dashboard.dataStatuses(),
                        REFERENCE_PERIOD_NOTES,
                        dashboard.festivalLocation(),
                        LIMITATION_NOTE
                )
        );
    }

    // §1 · 진단 시점 D-X와 시점별 활용 가능 데이터를 병기한다.
    private ForecastReportResponse.ReportHeroResponse hero(
            DiagnosisResponse diagnosis,
            Integer daysUntilStart,
            boolean forecastActive
    ) {
        return new ForecastReportResponse.ReportHeroResponse(
                diagnosis.festivalName(),
                diagnosis.startDate(),
                diagnosis.endDate(),
                daysUntilStart,
                daysUntilStart == null ? "진단 시점 산출 불가" : "진단 시점 · D-" + daysUntilStart
                        + (forecastActive ? " · 예측 데이터 활성" : " · 예측 데이터 없음"),
                forecastActive,
                forecastActive
                        ? List.of("API #1", "API #2", "API #6", "API #7", "API #8", "CSV #9")
                        : List.of("API #8", "CSV #9"),
                // 브리핑(AI 방향 C)은 ai-service가 채운다.
                null
        );
    }

    // §2 · A4 요약본
    private ForecastReportResponse.SummarySheetResponse summarySheet(
            DiagnosisResponse diagnosis,
            DiagnosisDashboardResponse dashboard
    ) {
        return new ForecastReportResponse.SummarySheetResponse(
                "축제날씨 진단 요약",
                diagnosis.festivalName(),
                diagnosis.startDate(),
                diagnosis.endDate(),
                diagnosis.areaCode(),
                diagnosis.signguCode(),
                diagnosis.festivalType() == null ? null : diagnosis.festivalType().name(),
                diagnosis.scale() == null ? null : diagnosis.scale().name(),
                keyFacts(dashboard),
                dashboard.risks().stream().limit(3).toList(),
                dashboard.recommendations().stream().limit(3).toList(),
                DATA_SOURCE_NOTE,
                LIMITATION_NOTE
        );
    }

    // 결재 첨부용 핵심 팩트. 데이터가 없는 항목은 문장 자체를 만들지 않는다.
    private List<String> keyFacts(DiagnosisDashboardResponse dashboard) {
        List<String> facts = new ArrayList<>();
        FestivalHistoryResponse history = dashboard.festivalHistory();
        if (history != null && history.lastYearVisitors() != null) {
            facts.add("작년 방문 " + history.lastYearVisitors().toPlainString() + "명 (문체부 실적)");
        }
        if (history != null && history.budgetMillionWon() != null) {
            facts.add("예산 " + history.budgetMillionWon().toPlainString() + "백만원 (문체부 실적)");
        }
        if (dashboard.volatility() != null && dashboard.volatility().totalCount() != null) {
            facts.add("변동 상황 관광지 " + dashboard.volatility().totalCount() + "곳");
        }
        if (dashboard.distribution() != null && dashboard.distribution().totalCount() != null) {
            facts.add("여유 관광지 " + dashboard.distribution().totalCount() + "곳");
        }
        if (dashboard.competing() != null && dashboard.competing().totalCount() != null) {
            facts.add("동기간 50km 이내 인근 축제 " + dashboard.competing().totalCount() + "건");
        }
        long criticalCount = dashboard.risks().stream()
                .filter(risk -> "CRITICAL".equals(risk.severity()))
                .count();
        facts.add("심각 리스크 " + criticalCount + "건 · 전체 리스크 " + dashboard.risks().size() + "건");
        return List.copyOf(facts);
    }

    // §3 · 4개 데이터 뷰와 수평 막대 4축을 그대로 전달한다.
    private ForecastReportResponse.DataSummaryResponse dataSummary(DiagnosisDashboardResponse dashboard) {
        return new ForecastReportResponse.DataSummaryResponse(
                dashboard.profile(),
                dashboard.concentration(),
                dashboard.volatility(),
                dashboard.distribution(),
                dashboard.competing(),
                dashboard.regionalVisitors(),
                dashboard.festivalHistory()
        );
    }

    // §5 · 운영 조정 제안 체크리스트
    private List<ForecastReportResponse.OperationChecklistItemResponse> checklist(
            List<RecommendationResponse> recommendations
    ) {
        return recommendations.stream()
                .map(recommendation -> new ForecastReportResponse.OperationChecklistItemResponse(
                        recommendation.recommendationCode(),
                        recommendation.priority(),
                        recommendation.category(),
                        recommendation.difficulty(),
                        recommendation.title(),
                        recommendation.defaultAction(),
                        recommendation.relatedRiskCodes(),
                        false
                ))
                .toList();
    }

    private Integer daysUntil(LocalDate startDate) {
        return startDate == null ? null : Math.toIntExact(ChronoUnit.DAYS.between(LocalDate.now(), startDate));
    }
}
