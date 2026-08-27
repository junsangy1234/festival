package com.junsang.festival.domain.diagnosis.service;

import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisResult;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
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
import com.junsang.festival.domain.diagnosis.dto.response.FestivalHistoryResponse;
import com.junsang.festival.domain.diagnosis.dto.response.FestivalLocationResponse;
import com.junsang.festival.domain.diagnosis.dto.response.MapResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RegionalVisitorResponse;
import com.junsang.festival.domain.diagnosis.dto.response.RelatedPlacesResponse;
import com.junsang.festival.domain.diagnosis.dto.response.VolatilityPlaceResponse;
import com.junsang.festival.domain.diagnosis.dto.response.VolatilityResponse;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.diagnosis.location.FestivalLocation;
import com.junsang.festival.domain.diagnosis.policy.PolicyEvaluationResult;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalSummaryResponse;
import com.junsang.festival.domain.festival.service.GeoDistanceService;
import com.junsang.festival.domain.festivalhistory.dto.FestivalHistoryRecord;
import com.junsang.festival.domain.festivalhistory.repository.FestivalHistoryRepository;
import com.junsang.festival.infra.tourapi.dto.hub.TourHubAttractionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// M2 대시보드 응답 조립
@Service
@RequiredArgsConstructor
public class DiagnosisDashboardAssembler {

    private static final DateTimeFormatter TOUR_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    // R-VOL-005·O-INF-003의 축제장 최인접 반경(기획서 5.5.1)
    private static final double NEAREST_RADIUS_KM = 1.0;
    // R-COMP-003 대규모 축제 기준(부록 A)
    private static final BigDecimal LARGE_FESTIVAL_VISITORS = BigDecimal.valueOf(100_000);

    private final DiagnosisPolicyEvaluationService policyEvaluationService;
    private final DashboardDisplayProperties dashboardDisplayProperties;
    private final FestivalHistoryRepository festivalHistoryRepository;
    private final GeoDistanceService geoDistanceService;

    // M2 대시보드 생성
    public DiagnosisDashboardResponse assemble(Diagnosis diagnosis, DiagnosisDataContext context) {
        PolicyEvaluationResult policy = policyEvaluationService.evaluate(context);
        ExternalDataResult<ConcentrationAnalysisResult> concentration = source(context, DiagnosisDataKeys.CONCENTRATION);
        ExternalDataResult<RegionalVisitorAnalysisResult> visitors = source(context, DiagnosisDataKeys.REGIONAL_VISITORS);
        ExternalDataResult<RelatedPlaceAnalysisResult> related = source(context, DiagnosisDataKeys.RELATED_PLACES);
        ExternalDataResult<List<TourHubAttractionItem>> hub = source(context, DiagnosisDataKeys.HUB_ATTRACTIONS);
        ExternalDataResult<CompetingFestivalResponse> competing = source(context, DiagnosisDataKeys.COMPETING_FESTIVALS);
        ExternalDataResult<FestivalHistoryRecord> history = source(context, DiagnosisDataKeys.FESTIVAL_HISTORY);
        FestivalLocation location = (FestivalLocation) context.sourceData().get(DiagnosisDataKeys.FESTIVAL_LOCATION);

        ConcentrationAnalysisResult concentrationData = concentration == null ? null : concentration.data();
        List<TourHubAttractionItem> hubData = hub == null || hub.data() == null ? List.of() : hub.data();
        CompetingFestivalResponse competingData = competing == null ? null : competing.data();

        return new DiagnosisDashboardResponse(
                diagnosis.getId(),
                diagnosis.getStatus(),
                DiagnosisResponse.from(diagnosis),
                festivalLocation(location),
                map(location, hubData, concentrationData, competingData),
                statuses(context),
                profile(context),
                concentration(concentrationData),
                volatility(concentrationData),
                distribution(concentrationData, related == null ? null : related.data(), hubData),
                regionalVisitors(visitors == null ? null : visitors.data()),
                relatedPlaces(related == null ? null : related.data()),
                competing(competingData),
                festivalHistory(history == null ? null : history.data()),
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

    // 축제장 위치 변환(기획서 5.5)
    private FestivalLocationResponse festivalLocation(FestivalLocation location) {
        return location == null ? null : new FestivalLocationResponse(
                location.latitude(),
                location.longitude(),
                location.source().name(),
                location.precise(),
                location.address(),
                location.notice()
        );
    }

    // 지도 표현(기획서 5.5) · 축제장 마커 + 관광지 마커(API #6 좌표 × API #1 집중률) + 인근 축제 마커
    private MapResponse map(
            FestivalLocation location,
            List<TourHubAttractionItem> hubAttractions,
            ConcentrationAnalysisResult concentration,
            CompetingFestivalResponse competing
    ) {
        if (location == null || !location.hasCoordinates()) {
            return new MapResponse(
                    null,
                    NEAREST_RADIUS_KM,
                    List.of(),
                    List.of(),
                    location == null ? null : location.notice()
            );
        }
        return new MapResponse(
                new MapResponse.SiteMarkerResponse(
                        location.latitude(),
                        location.longitude(),
                        location.source().name(),
                        location.precise(),
                        location.address()
                ),
                NEAREST_RADIUS_KM,
                placeMarkers(location, hubAttractions, concentration),
                festivalMarkers(location, competing),
                location.notice()
        );
    }

    // 관광지 마커. 좌표가 있는 중심 관광지에만 집중률 배지를 붙인다.
    private List<MapResponse.PlaceMarkerResponse> placeMarkers(
            FestivalLocation location,
            List<TourHubAttractionItem> hubAttractions,
            ConcentrationAnalysisResult concentration
    ) {
        Map<String, ConcentrationAnalysisResult.VolatilityPlace> volatilityByName = new LinkedHashMap<>();
        Set<String> relaxedNames = new LinkedHashSet<>();
        if (concentration != null) {
            concentration.volatilityPlaces()
                    .forEach(place -> volatilityByName.putIfAbsent(place.placeName(), place));
            concentration.relaxedPlaceCandidates()
                    .forEach(place -> relaxedNames.add(place.placeName()));
        }

        return hubAttractions.stream()
                .filter(TourHubAttractionItem::hasCoordinates)
                .map(item -> {
                    var volatility = volatilityByName.get(item.placeName());
                    BigDecimal distanceKm = geoDistanceService.calculateKm(
                            location.latitude(), location.longitude(), item.latitude(), item.longitude()
                    );
                    return new MapResponse.PlaceMarkerResponse(
                            item.placeName(),
                            item.latitude(),
                            item.longitude(),
                            item.largeCategory(),
                            item.hubRank(),
                            volatility != null ? volatility.badge().name()
                                    : relaxedNames.contains(item.placeName()) ? "RELAXED" : null,
                            volatility == null ? null : doubleValue(volatility.peakDelta()),
                            distanceKm,
                            location.precise() && distanceKm.doubleValue() <= NEAREST_RADIUS_KM
                    );
                })
                .sorted(java.util.Comparator.comparing(MapResponse.PlaceMarkerResponse::distanceKm))
                .toList();
    }

    // 동기간 인근 축제 마커
    private List<MapResponse.FestivalMarkerResponse> festivalMarkers(
            FestivalLocation location,
            CompetingFestivalResponse competing
    ) {
        if (competing == null) {
            return List.of();
        }
        return competing.festivals().stream()
                .limit(dashboardDisplayProperties.maxCompetingFestivals())
                .map(festival -> new MapResponse.FestivalMarkerResponse(
                        festival.contentId(),
                        festival.title(),
                        coordinate(festival.latitude()),
                        coordinate(festival.longitude()),
                        festival.distanceKm(),
                        parseDate(festival.eventStartDate()),
                        parseDate(festival.eventEndDate())
                ))
                .toList();
    }

    // 상단 프로필 구성 · 수평 막대 4축(API #1·#2만 사용)
    private DataProfileResponse profile(DiagnosisDataContext context) {
        return new DataProfileResponse(
                metric(context, "FESTIVAL_PERIOD_AVERAGE_CONCENTRATION", 0, 100, "", "HIGHER"),
                metric(context, "RELAXED_PLACE_COUNT", 0, 10, "곳", "HIGHER"),
                metric(context, "RELATED_AVERAGE_TOP_TEN_RANK", 1, 50, "순위", "LOWER"),
                metric(context, "RELATED_CATEGORY_DIVERSITY", 0, null, "개", "HIGHER"),
                DataProfileResponse.REQUIRED_NOTES
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
                                doubleValue(place.deltaFromSelfAverage())
                        )).toList()
                )).toList());
    }

    // 변동성 목록 구성
    private VolatilityResponse volatility(ConcentrationAnalysisResult result) {
        if (result == null) {
            return new VolatilityResponse(null, 0, List.of());
        }
        return new VolatilityResponse(
                "관광지별 자기평균(26일 예측) 대비 개최기간 최고 상승폭",
                result.volatilityPlaces().size(),
                result.volatilityPlaces().stream().limit(dashboardDisplayProperties.maxVolatilityPlaces())
                        .map(place -> new VolatilityPlaceResponse(
                        place.placeName(), doubleValue(place.peakDelta()), place.badge().name(),
                        doubleValue(place.selfAverage()), doubleValue(place.peakRate()), place.peakDate()
                )).toList()
        );
    }

    // 여유 관광지 목록 구성 · 유형·연관 순위(API #2)와 hubRank(API #6)를 함께 표시한다.
    private DistributionResponse distribution(
            ConcentrationAnalysisResult result,
            RelatedPlaceAnalysisResult related,
            List<TourHubAttractionItem> hubAttractions
    ) {
        if (result == null) {
            return new DistributionResponse(null, 0, List.of());
        }
        Map<String, RelatedPlaceAnalysisResult.RelatedPlace> relatedByName = relatedByName(related);
        Map<String, TourHubAttractionItem> hubByName = new LinkedHashMap<>();
        hubAttractions.forEach(item -> hubByName.putIfAbsent(item.placeName(), item));

        List<DistributionPlaceResponse> places = java.util.stream.IntStream
                .range(0, Math.min(result.relaxedPlaceCandidates().size(), dashboardDisplayProperties.maxDistributionPlaces()))
                .mapToObj(index -> {
                    var place = result.relaxedPlaceCandidates().get(index);
                    var relatedPlace = relatedByName.get(place.placeName());
                    var hubPlace = hubByName.get(place.placeName());
                    return new DistributionPlaceResponse(
                            index + 1,
                            place.placeName(),
                            relatedPlace != null ? relatedPlace.largeCategory()
                                    : hubPlace == null ? null : hubPlace.largeCategory(),
                            relatedPlace == null ? null : relatedPlace.rank(),
                            hubPlace == null ? null : hubPlace.hubRank(),
                            "개최기간 평균이 자기평균보다 5%p 이상 낮고 집중률이 40 이하",
                            doubleValue(place.festivalPeriodAverage())
                    );
                }).toList();
        return new DistributionResponse(
                "개최기간 혼잡이 상대적으로 낮은 관광지", result.relaxedPlaceCandidates().size(), places
        );
    }

    // 연관 관광지에서 관광지명별 최상위 순위 항목을 뽑는다.
    private Map<String, RelatedPlaceAnalysisResult.RelatedPlace> relatedByName(RelatedPlaceAnalysisResult related) {
        Map<String, RelatedPlaceAnalysisResult.RelatedPlace> byName = new LinkedHashMap<>();
        if (related == null) {
            return byName;
        }
        related.basePlaces().forEach(base -> base.relatedPlaces().forEach(place -> byName.merge(
                place.placeName(), place, (left, right) -> left.rank() <= right.rank() ? left : right
        )));
        return byName;
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

    // 동기간 인근 축제 구성 · API #8 정보에 CSV #9 실측을 조인하고 연계 태그를 붙인다.
    private CompetingResponse competing(CompetingFestivalResponse result) {
        if (result == null) {
            return new CompetingResponse(0, 0, List.of(), 0);
        }
        List<CompetitionFestivalResponse> festivals = result.festivals().stream()
                .limit(dashboardDisplayProperties.maxCompetingFestivals())
                .map(this::toCompetitionFestival)
                .toList();
        return new CompetingResponse(
                result.count(),
                festivals.size(),
                festivals,
                result.excludedMissingCoordinatesCount()
        );
    }

    private CompetitionFestivalResponse toCompetitionFestival(FestivalSummaryResponse festival) {
        FestivalHistoryRecord history = festivalHistoryRepository
                .findByFestivalName(festival.title())
                .orElse(null);
        return new CompetitionFestivalResponse(
                festival.contentId(),
                festival.title(),
                festival.address(),
                parseDate(festival.eventStartDate()),
                parseDate(festival.eventEndDate()),
                festival.addressDetail(),
                festival.distanceKm(),
                history == null ? null : history.lastYearVisitors(),
                history == null ? null : history.budgetMillionWon(),
                linkageTags(festival, history)
        );
    }

    // 연계 가능성 태그(기획서 뷰 04 · 경합에서 연계 프레임으로 전환)
    private List<String> linkageTags(FestivalSummaryResponse festival, FestivalHistoryRecord history) {
        List<String> tags = new ArrayList<>();
        if (festival.distanceKm() != null && festival.distanceKm().doubleValue() <= 30) {
            tags.add("광역 관광 코스");
        }
        if (history != null && history.lastYearVisitors() != null
                && history.lastYearVisitors().compareTo(LARGE_FESTIVAL_VISITORS) >= 0) {
            tags.add("일자 겹침 유의");
        }
        tags.add("상호 홍보 가능");
        return List.copyOf(tags);
    }

    // 재개최 실적 카드(CSV #9)
    private FestivalHistoryResponse festivalHistory(FestivalHistoryRecord record) {
        return record == null ? null : new FestivalHistoryResponse(
                record.festivalName(),
                record.regionName(),
                record.signguName(),
                record.lastYearVisitors(),
                record.budgetMillionWon(),
                record.firstHeldYear(),
                record.roundCount()
        );
    }

    // 관광공사 날짜 변환
    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, TOUR_DATE);
    }

    // 관광공사 좌표 변환
    private BigDecimal coordinate(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // 화면 숫자 변환
    private Double doubleValue(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
