package com.junsang.festival.domain.diagnosis.collection;

import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisService;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisService;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisService;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import com.junsang.festival.domain.diagnosis.data.ExternalDataResult;
import com.junsang.festival.domain.diagnosis.data.ExternalDataStatus;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.diagnosis.location.FestivalLocation;
import com.junsang.festival.domain.diagnosis.location.FestivalLocationService;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalSummaryResponse;
import com.junsang.festival.domain.festival.service.FestivalService;
import com.junsang.festival.domain.festival.service.GeoDistanceService;
import com.junsang.festival.domain.festivalhistory.dto.FestivalHistoryRecord;
import com.junsang.festival.domain.festivalhistory.repository.FestivalHistoryRepository;
import com.junsang.festival.infra.tourapi.client.TourConcentrationClient;
import com.junsang.festival.infra.tourapi.client.TourHubAttractionClient;
import com.junsang.festival.infra.tourapi.client.TourRegionalVisitorClient;
import com.junsang.festival.infra.tourapi.client.TourRelatedPlaceClient;
import com.junsang.festival.infra.tourapi.config.TourExternalDataProperties;
import com.junsang.festival.infra.tourapi.dto.hub.TourHubAttractionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 외부 데이터 수집·정제. MVP 데이터 스택은 API #1·#2·#6·#7·#8 + CSV #9다.
@Component
@RequiredArgsConstructor
public class DefaultDiagnosisDataCollector implements DiagnosisDataCollector {

    // API #1은 조회일 기준 D+0 ~ D+25(26일)만 반환한다.
    private static final int FORECAST_WINDOW_DAYS = 25;
    // R-VOL-005 · 축제장 최인접 판정 반경
    private static final double NEAREST_PLACE_RADIUS_KM = 1.0;
    // R-COMP-003 · 부록 A 기준 대규모 축제(방문 10만+)
    private static final BigDecimal LARGE_FESTIVAL_VISITORS = BigDecimal.valueOf(100_000);

    private final TourConcentrationClient concentrationClient;
    private final TourRelatedPlaceClient relatedPlaceClient;
    private final TourRegionalVisitorClient regionalVisitorClient;
    private final TourHubAttractionClient hubAttractionClient;
    private final ConcentrationAnalysisService concentrationAnalysisService;
    private final RelatedPlaceAnalysisService relatedPlaceAnalysisService;
    private final RegionalVisitorAnalysisService regionalVisitorAnalysisService;
    private final FestivalLocationService festivalLocationService;
    private final FestivalHistoryRepository festivalHistoryRepository;
    private final FestivalService festivalService;
    private final GeoDistanceService geoDistanceService;
    private final TourExternalDataProperties externalDataProperties;

    // 진단 데이터 수집
    @Override
    public DiagnosisDataContext collect(Diagnosis diagnosis) {
        Map<String, Object> sourceData = new LinkedHashMap<>();
        List<DiagnosisMetric> metrics = new ArrayList<>();

        ExternalDataResult<ConcentrationAnalysisResult> concentration = concentration(diagnosis);
        sourceData.put(DiagnosisDataKeys.CONCENTRATION, concentration);
        addConcentrationMetrics(concentration, metrics);

        ExternalDataResult<RegionalVisitorAnalysisResult> visitors = visitors(diagnosis);
        sourceData.put(DiagnosisDataKeys.REGIONAL_VISITORS, visitors);
        addVisitorMetrics(visitors, metrics);

        ExternalDataResult<RelatedPlaceAnalysisResult> related = related(diagnosis);
        sourceData.put(DiagnosisDataKeys.RELATED_PLACES, related);
        addRelatedMetrics(related, metrics);

        ExternalDataResult<List<TourHubAttractionItem>> hubAttractions = hubAttractions(diagnosis);
        sourceData.put(DiagnosisDataKeys.HUB_ATTRACTIONS, hubAttractions);

        FestivalLocation location = festivalLocationService.resolve(
                diagnosis, hubAttractions.data() == null ? List.of() : hubAttractions.data()
        );
        sourceData.put(DiagnosisDataKeys.FESTIVAL_LOCATION, location);
        addNearestPlaceMetric(location, hubAttractions.data(), concentration.data(), metrics);

        ExternalDataResult<CompetingFestivalResponse> competing = competing(diagnosis, location);
        sourceData.put(DiagnosisDataKeys.COMPETING_FESTIVALS, competing);
        addCompetingMetrics(competing, metrics);

        ExternalDataResult<FestivalHistoryRecord> history = festivalHistory(diagnosis);
        sourceData.put(DiagnosisDataKeys.FESTIVAL_HISTORY, history);
        addFestivalHistoryMetrics(history, metrics);

        addDataStatusMetrics(sourceData, history, metrics);

        return new DiagnosisDataContext(diagnosis, Map.copyOf(sourceData), List.copyOf(metrics));
    }

    // R-DATA-* · 데이터가 빠진 사실 자체를 리스크로 노출한다(기획서 5.2 · 부록 A.2).
    // 화면이 조용히 비어 있는 대신 "무엇이 왜 빠졌는지"가 리포트에 남는다.
    private void addDataStatusMetrics(
            Map<String, Object> sourceData,
            ExternalDataResult<FestivalHistoryRecord> history,
            List<DiagnosisMetric> metrics
    ) {
        List<String> unusable = sourceData.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof ExternalDataResult<?>)
                .filter(entry -> {
                    ExternalDataStatus status = ((ExternalDataResult<?>) entry.getValue()).status();
                    return status == ExternalDataStatus.FAILED
                            || status == ExternalDataStatus.OUT_OF_FORECAST_RANGE;
                })
                .map(Map.Entry::getKey)
                .toList();
        metrics.add(new DiagnosisMetric(
                "UNUSABLE_DATA_SOURCE_COUNT",
                BigDecimal.valueOf(unusable.size()),
                Map.of("sourceNames", String.join(", ", unusable))
        ));

        metrics.add(new DiagnosisMetric(
                "FESTIVAL_NAME_MATCH_FAILED",
                history.data() == null ? BigDecimal.ONE : BigDecimal.ZERO,
                Map.of()
        ));
    }

    private ExternalDataResult<ConcentrationAnalysisResult> concentration(Diagnosis diagnosis) {
        LocalDate today = LocalDate.now();
        String period = diagnosis.getStartDate() + "~" + diagnosis.getEndDate();
        if (diagnosis.getStartDate().isBefore(today)
                || diagnosis.getStartDate().isAfter(today.plusDays(FORECAST_WINDOW_DAYS))) {
            return ExternalDataResult.outOfRange(
                    "관광지 집중률은 조회일 기준 향후 26일(D+0~D+25)만 제공합니다.", period
            );
        }
        try {
            var items = concentrationClient.getForecastItems(diagnosis.getAreaCode(), diagnosis.getSignguCode());
            if (items.isEmpty()) {
                return ExternalDataResult.noData("개최 지역의 집중률 예측 데이터가 없습니다.", period);
            }
            return ExternalDataResult.available(
                    period,
                    concentrationAnalysisService.analyze(items, diagnosis.getStartDate(), diagnosis.getEndDate())
            );
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
    }

    private ExternalDataResult<RegionalVisitorAnalysisResult> visitors(Diagnosis diagnosis) {
        int yearsBack = Math.max(1, externalDataProperties.visitorReferenceYearsBack());
        LocalDate referenceStart = diagnosis.getStartDate().minusYears(yearsBack);
        LocalDate referenceEnd = diagnosis.getEndDate().minusYears(yearsBack);
        String period = referenceStart + "~" + referenceEnd + " (과거 참고 기간)";
        try {
            var items = regionalVisitorClient.getAll(
                    diagnosis.getSignguCode(), referenceStart.minusDays(7), referenceEnd.plusDays(7)
            );
            if (items.isEmpty()) {
                return ExternalDataResult.noData("해당 시군구의 과거 방문자 데이터가 없습니다.", period);
            }
            return ExternalDataResult.available(
                    period,
                    regionalVisitorAnalysisService.analyze(items, referenceStart, referenceEnd)
            );
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
    }

    private ExternalDataResult<RelatedPlaceAnalysisResult> related(Diagnosis diagnosis) {
        String period = externalDataProperties.resolvedRelatedBaseYearMonth();
        try {
            var items = relatedPlaceClient.getAll(diagnosis.getAreaCode(), diagnosis.getSignguCode());
            if (items.isEmpty()) {
                return ExternalDataResult.noData("개최 지역의 연관 관광지 데이터가 없습니다.", period);
            }
            return ExternalDataResult.available(period, relatedPlaceAnalysisService.analyze(items));
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
    }

    // API #6 · 뷰 03 강화와 R-VOL-005 좌표 정밀화에 쓰는 중심 관광지 목록
    private ExternalDataResult<List<TourHubAttractionItem>> hubAttractions(Diagnosis diagnosis) {
        String period = externalDataProperties.resolvedRelatedBaseYearMonth();
        try {
            List<TourHubAttractionItem> items =
                    hubAttractionClient.getAll(diagnosis.getAreaCode(), diagnosis.getSignguCode());
            if (items.isEmpty()) {
                return ExternalDataResult.noData("개최 지역의 중심 관광지 데이터가 없습니다.", period);
            }
            return ExternalDataResult.available(period, items);
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
    }

    private ExternalDataResult<CompetingFestivalResponse> competing(Diagnosis diagnosis, FestivalLocation location) {
        String period = diagnosis.getStartDate() + "~" + diagnosis.getEndDate();
        if (!location.hasCoordinates()) {
            return ExternalDataResult.failed("기준 좌표가 없어 인근 축제 거리를 계산할 수 없습니다.", period);
        }
        try {
            CompetingFestivalResponse result = festivalService.findCompetingFestivals(
                    diagnosis.getExistingFestivalContentId(),
                    diagnosis.getStartDate(),
                    diagnosis.getEndDate(),
                    location.latitude(),
                    location.longitude()
            );
            return result.festivals().isEmpty()
                    ? ExternalDataResult.noData("50km 이내 동기간 인근 축제가 없습니다.", period)
                    : ExternalDataResult.available(period, result);
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
    }

    // CSV #9 · 재개최 실적(방문객수·예산)을 축제명으로 조인한다.
    private ExternalDataResult<FestivalHistoryRecord> festivalHistory(Diagnosis diagnosis) {
        String period = "문체부 지역축제 개최 계획 현황 (전년 실적)";
        Optional<FestivalHistoryRecord> record =
                festivalHistoryRepository.findByFestivalName(diagnosis.getFestivalName());
        return record
                .map(value -> ExternalDataResult.available(period, value))
                .orElseGet(() -> ExternalDataResult.noData("문체부 재개최 실적에서 축제명을 찾지 못했습니다.", period));
    }

    private void addConcentrationMetrics(
            ExternalDataResult<ConcentrationAnalysisResult> result,
            List<DiagnosisMetric> metrics
    ) {
        if (result.data() == null) {
            return;
        }
        if (result.data().festivalPeriodAverage() != null) {
            metrics.add(new DiagnosisMetric(
                    "FESTIVAL_PERIOD_AVERAGE_CONCENTRATION",
                    result.data().festivalPeriodAverage(),
                    Map.of()
            ));
        }
        result.data().volatilityPlaces().forEach(place -> metrics.add(new DiagnosisMetric(
                "VOLATILITY_PEAK_INCREASE",
                place.peakDelta(),
                Map.of("placeName", place.placeName(), "date", place.peakDate().toString())
        )));
        metrics.add(new DiagnosisMetric(
                "RELAXED_PLACE_COUNT",
                BigDecimal.valueOf(result.data().relaxedPlaceCandidates().size()),
                Map.of()
        ));
    }

    // R-VOL-005 · 축제장 최인접(1km) 관광지의 상승폭. 좌표가 근사치면 5.5.3에 따라 판정을 건너뛴다.
    private void addNearestPlaceMetric(
            FestivalLocation location,
            List<TourHubAttractionItem> hubAttractions,
            ConcentrationAnalysisResult concentration,
            List<DiagnosisMetric> metrics
    ) {
        if (!location.precise() || hubAttractions == null || concentration == null) {
            return;
        }
        Map<String, TourHubAttractionItem> hubByName = new LinkedHashMap<>();
        hubAttractions.stream()
                .filter(TourHubAttractionItem::hasCoordinates)
                .forEach(item -> hubByName.putIfAbsent(item.placeName(), item));

        concentration.volatilityPlaces().stream()
                .filter(place -> hubByName.containsKey(place.placeName()))
                .map(place -> Map.entry(place, hubByName.get(place.placeName())))
                .filter(entry -> geoDistanceService.isWithinKm(
                        location.latitude(), location.longitude(),
                        entry.getValue().latitude(), entry.getValue().longitude(),
                        NEAREST_PLACE_RADIUS_KM
                ))
                .max(Map.Entry.comparingByKey(
                        java.util.Comparator.comparing(ConcentrationAnalysisResult.VolatilityPlace::peakDelta)
                ))
                .ifPresent(entry -> metrics.add(new DiagnosisMetric(
                        "NEAREST_PLACE_PEAK_INCREASE",
                        entry.getKey().peakDelta(),
                        Map.of(
                                "placeName", entry.getKey().placeName(),
                                "date", entry.getKey().peakDate().toString(),
                                "distanceKm", geoDistanceService.calculateKm(
                                        location.latitude(), location.longitude(),
                                        entry.getValue().latitude(), entry.getValue().longitude()
                                ).toPlainString()
                        )
                )));
    }

    private void addVisitorMetrics(
            ExternalDataResult<RegionalVisitorAnalysisResult> result,
            List<DiagnosisMetric> metrics
    ) {
        if (result.data() != null && result.data().festivalPeriodAverage() != null) {
            metrics.add(new DiagnosisMetric(
                    "REGIONAL_VISITOR_PERIOD_AVERAGE",
                    result.data().festivalPeriodAverage(),
                    Map.of("referencePeriod", result.referencePeriod())
            ));
        }
    }

    private void addRelatedMetrics(
            ExternalDataResult<RelatedPlaceAnalysisResult> result,
            List<DiagnosisMetric> metrics
    ) {
        if (result.data() == null) {
            return;
        }
        if (result.data().averageTopTenRank() != null) {
            metrics.add(new DiagnosisMetric("RELATED_AVERAGE_TOP_TEN_RANK", result.data().averageTopTenRank(), Map.of()));
        }
        metrics.add(new DiagnosisMetric(
                "RELATED_CATEGORY_DIVERSITY",
                BigDecimal.valueOf(result.data().categoryDiversity()),
                Map.of()
        ));
    }

    private void addCompetingMetrics(
            ExternalDataResult<CompetingFestivalResponse> result,
            List<DiagnosisMetric> metrics
    ) {
        List<FestivalSummaryResponse> festivals =
                result.data() == null ? List.of() : result.data().festivals();
        String names = festivals.stream()
                .map(FestivalSummaryResponse::title)
                .collect(java.util.stream.Collectors.joining(", "));
        metrics.add(new DiagnosisMetric(
                "COMPETING_FESTIVAL_COUNT_50KM",
                BigDecimal.valueOf(festivals.size()),
                Map.of("festivalNames", names)
        ));

        // R-COMP-003 · CSV #9 실측 방문객수로 인근 대규모 축제를 판정한다.
        List<String> largeFestivals = festivals.stream()
                .filter(festival -> festivalHistoryRepository.findByFestivalName(festival.title())
                        .map(FestivalHistoryRecord::lastYearVisitors)
                        .filter(visitors -> visitors.compareTo(LARGE_FESTIVAL_VISITORS) >= 0)
                        .isPresent())
                .map(FestivalSummaryResponse::title)
                .toList();
        metrics.add(new DiagnosisMetric(
                "COMPETING_LARGE_FESTIVAL_COUNT",
                BigDecimal.valueOf(largeFestivals.size()),
                Map.of("festivalNames", String.join(", ", largeFestivals))
        ));
    }

    // R-YEAR-002·003 · CSV #9 한 해 실적만으로 판정 가능한 지표
    private void addFestivalHistoryMetrics(
            ExternalDataResult<FestivalHistoryRecord> result,
            List<DiagnosisMetric> metrics
    ) {
        FestivalHistoryRecord record = result.data();
        if (record == null) {
            return;
        }
        if (record.lastYearVisitors() != null) {
            metrics.add(new DiagnosisMetric(
                    "FESTIVAL_HISTORY_LAST_YEAR_VISITORS",
                    record.lastYearVisitors(),
                    Map.of("festivalName", record.festivalName())
            ));
        }
        if (record.firstHeldYear() != null) {
            metrics.add(new DiagnosisMetric(
                    "FESTIVAL_HISTORY_YEARS_SINCE_FIRST",
                    BigDecimal.valueOf(Year.now().getValue() - record.firstHeldYear()),
                    Map.of("firstHeldYear", String.valueOf(record.firstHeldYear()))
            ));
        }
    }

    private String safeReason(RuntimeException exception) {
        return exception.getMessage() == null ? "외부 데이터를 처리하지 못했습니다." : exception.getMessage();
    }
}
