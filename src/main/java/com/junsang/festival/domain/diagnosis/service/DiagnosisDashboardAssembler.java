package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisResult;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import com.junsang.festival.domain.diagnosis.collection.DefaultDiagnosisDataCollector.TargetLocation;
import com.junsang.festival.domain.diagnosis.collection.DiagnosisDataKeys;
import com.junsang.festival.domain.diagnosis.config.DashboardDisplayProperties;
import com.junsang.festival.domain.diagnosis.data.ExternalDataResult;
import com.junsang.festival.domain.diagnosis.dto.response.CompetingResponse;
import com.junsang.festival.domain.diagnosis.dto.response.CompetitionFestivalResponse;
import com.junsang.festival.domain.diagnosis.dto.response.ConcentrationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.ConcentrationSpotResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DataProfileMetricResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DataProfileResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisDashboardResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DiagnosisResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DistributionPlaceResponse;
import com.junsang.festival.domain.diagnosis.dto.response.DistributionResponse;
import com.junsang.festival.domain.diagnosis.dto.response.ExternalDataStatusResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RegionalVisitorResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RelatedPlacesResponse;
import com.junsang.festival.domain.diagnosis.dto.response.TargetLocationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.VolatilityPlaceResponse;
import com.junsang.festival.domain.diagnosis.dto.response.VolatilityResponse;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.diagnosis.policy.PolicyEvaluationResult;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// M2 대시보드 응답 조립
@Service
@RequiredArgsConstructor
public class DiagnosisDashboardAssembler {

    private static final DateTimeFormatter TOUR_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final DiagnosisPolicyEvaluationService policyEvaluationService;
    private final DashboardDisplayProperties dashboardDisplayProperties;

    // M2 대시보드 생성
    public DiagnosisDashboardResponse assemble(Diagnosis diagnosis, DiagnosisDataContext context) {
        PolicyEvaluationResult policy = policyEvaluationService.evaluate(context);
        ExternalDataResult<ConcentrationAnalysisResult> concentration = source(context, DiagnosisDataKeys.CONCENTRATION);
        ExternalDataResult<RegionalVisitorAnalysisResult> visitors = source(context, DiagnosisDataKeys.REGIONAL_VISITORS);
        ExternalDataResult<RelatedPlaceAnalysisResult> related = source(context, DiagnosisDataKeys.RELATED_PLACES);
        ExternalDataResult<CompetingFestivalResponse> competing = source(context, DiagnosisDataKeys.COMPETING_FESTIVALS);
        TargetLocation location = (TargetLocation) context.sourceData().get(DiagnosisDataKeys.TARGET_LOCATION);

        return new DiagnosisDashboardResponse(
                diagnosis.getId(),
                diagnosis.getStatus(),
                DiagnosisResponse.from(diagnosis),
                targetLocation(location),
                statuses(context),
                profile(context),
                concentration(concentration == null ? null : concentration.data()),
                volatility(concentration == null ? null : concentration.data()),
                distribution(concentration == null ? null : concentration.data()),
                regionalVisitors(visitors == null ? null : visitors.data()),
                relatedPlaces(related == null ? null : related.data()),
                competing(competing == null ? null : competing.data()),
                policy.risks(),
                policy.recommendations()
        );
    }

    @SuppressWarnings("unchecked")
    // 외부 데이터 결과 추출
    private <T> ExternalDataResult<T> source(DiagnosisDataContext context, String key) {
        Object value = context.sourceData().get(key);
        return value instanceof ExternalDataResult<?> result ? (ExternalDataResult<T>) result : null;
    }

    // 외부 데이터 상태 변환
    private List<ExternalDataStatusResponse> statuses(DiagnosisDataContext context) {
        List<ExternalDataStatusResponse> statuses = new ArrayList<>();
        context.sourceData().forEach((source, value) -> {
            if (value instanceof ExternalDataResult<?> result) {
                statuses.add(new ExternalDataStatusResponse(
                        source, result.status(), result.reason(), result.referencePeriod(), result.retrievedAt()
                ));
            }
        });
        return List.copyOf(statuses);
    }

    // 기준 좌표 변환
    private TargetLocationResponse targetLocation(TargetLocation location) {
        return location == null ? null : new TargetLocationResponse(
                location.latitude(), location.longitude(), location.source()
        );
    }

    // 상단 프로필 구성
    private DataProfileResponse profile(DiagnosisDataContext context) {
        return new DataProfileResponse(
                metric(context, "FESTIVAL_PERIOD_AVERAGE_CONCENTRATION", 0, 100, "%", "HIGHER"),
                metric(context, "RELAXED_PLACE_COUNT", 0, null, "곳", "HIGHER"),
                metric(context, "RELATED_AVERAGE_TOP_TEN_RANK", 1, 10, "순위", "LOWER"),
                metric(context, "RELATED_CATEGORY_DIVERSITY", 0, null, "개", "HIGHER")
        );
    }

    // 프로필 지표 변환
    private DataProfileMetricResponse metric(
            DiagnosisDataContext context,
            String code,
            Number minimum,
            Number maximum,
            String unit,
            String direction
    ) {
        return context.metrics().stream()
                .filter(metric -> code.equals(metric.code()))
                .findFirst()
                .map(DiagnosisMetric::value)
                .map(value -> new DataProfileMetricResponse(value, minimum, maximum, unit, direction))
                .orElse(null);
    }

    // 집중률 추이 구성
    private ConcentrationResponse concentration(ConcentrationAnalysisResult result) {
        if (result == null) {
            return new ConcentrationResponse(List.of());
        }
        Set<String> selectedPlaces = result.volatilityPlaces().stream()
                .limit(dashboardDisplayProperties.maxConcentrationPlaces())
                .map(ConcentrationAnalysisResult.VolatilityPlace::placeName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new ConcentrationResponse(result.dailyConcentrations().stream()
                .map(daily -> new ConcentrationResponse.DailyConcentrationResponse(
                        daily.date(),
                        daily.places().size(),
                        daily.places().stream()
                                .filter(place -> selectedPlaces.contains(place.placeName()))
                                .map(place -> new ConcentrationSpotResponse(
                                place.placeName(), doubleValue(place.concentrationRate()),
                                doubleValue(place.deltaFromThirtyDayAverage())
                        )).toList()
                )).toList());
    }

    // 변동성 목록 구성
    private VolatilityResponse volatility(ConcentrationAnalysisResult result) {
        if (result == null) {
            return new VolatilityResponse(null, 0, List.of());
        }
        return new VolatilityResponse(
                "관광지별 30일 평균 대비 축제기간 최고 상승폭",
                result.volatilityPlaces().size(),
                result.volatilityPlaces().stream().limit(dashboardDisplayProperties.maxVolatilityPlaces())
                        .map(place -> new VolatilityPlaceResponse(
                        place.placeName(), doubleValue(place.peakDelta()), place.badge().name(),
                        doubleValue(place.thirtyDayAverage()), doubleValue(place.peakRate()), place.peakDate()
                )).toList()
        );
    }

    // 여유 관광지 목록 구성
    private DistributionResponse distribution(ConcentrationAnalysisResult result) {
        if (result == null) {
            return new DistributionResponse(null, 0, List.of());
        }
        List<DistributionPlaceResponse> places = java.util.stream.IntStream
                .range(0, Math.min(result.relaxedPlaceCandidates().size(), dashboardDisplayProperties.maxDistributionPlaces()))
                .mapToObj(index -> {
                    var place = result.relaxedPlaceCandidates().get(index);
                    return new DistributionPlaceResponse(
                            index + 1,
                            place.placeName(),
                            "축제기간 평균이 30일 평균보다 5%p 이상 낮고 집중률이 40 이하",
                            doubleValue(place.festivalPeriodAverage())
                    );
                }).toList();
        return new DistributionResponse(
                "축제기간 혼잡이 상대적으로 낮은 관광지", result.relaxedPlaceCandidates().size(), places
        );
    }

    // 지역 방문자 구성
    private RegionalVisitorResponse regionalVisitors(RegionalVisitorAnalysisResult result) {
        if (result == null) {
            return new RegionalVisitorResponse(null, null, List.of(), null, null, null, null);
        }
        return new RegionalVisitorResponse(
                result.referenceFestivalStartDate(),
                result.referenceFestivalEndDate(),
                result.dailyVisitors().stream().map(daily -> new RegionalVisitorResponse.DailyVisitorResponse(
                        daily.date(), daily.localVisitors(), daily.outsideVisitors(), daily.foreignVisitors(),
                        daily.totalVisitors()
                )).toList(),
                result.festivalPeriodAverage(),
                result.beforePeriodAverage(),
                result.afterPeriodAverage(),
                result.changeFromBeforePercent()
        );
    }

    // 연관 관광지 구성
    private RelatedPlacesResponse relatedPlaces(RelatedPlaceAnalysisResult result) {
        if (result == null) {
            return new RelatedPlacesResponse(0, List.of(), null, 0);
        }
        return new RelatedPlacesResponse(
                result.basePlaces().size(),
                result.basePlaces().stream().limit(dashboardDisplayProperties.maxRelatedBasePlaces())
                        .map(base -> new RelatedPlacesResponse.BasePlaceResponse(
                        base.placeCode(),
                        base.placeName(),
                        base.relatedPlaces().size(),
                        base.relatedPlaces().stream().limit(dashboardDisplayProperties.maxRelatedPlacesPerBase())
                                .map(place -> new RelatedPlacesResponse.RelatedPlaceResponse(
                                place.placeCode(), place.placeName(), place.regionName(), place.signguName(),
                                place.largeCategory(), place.mediumCategory(), place.smallCategory(), place.rank()
                        )).toList()
                )).toList(),
                result.averageTopTenRank(),
                result.categoryDiversity()
        );
    }

    // 경합 축제 구성
    private CompetingResponse competing(CompetingFestivalResponse result) {
        if (result == null) {
            return new CompetingResponse(0, 0, List.of(), 0);
        }
        List<CompetitionFestivalResponse> festivals = result.festivals().stream()
                .limit(dashboardDisplayProperties.maxCompetingFestivals())
                .map(festival -> new CompetitionFestivalResponse(
                        festival.contentId(),
                        festival.title(),
                        festival.address(),
                        parseDate(festival.eventStartDate()),
                        parseDate(festival.eventEndDate()),
                        festival.addressDetail(),
                        festival.distanceKm()
                )).toList();
        return new CompetingResponse(
                result.count(),
                festivals.size(),
                festivals,
                result.excludedMissingCoordinatesCount()
        );
    }

    // 관광공사 날짜 변환
    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, TOUR_DATE);
    }

    // 화면 숫자 변환
    private Double doubleValue(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
