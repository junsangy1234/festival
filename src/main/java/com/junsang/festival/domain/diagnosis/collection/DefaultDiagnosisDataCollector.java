package com.junsang.festival.domain.diagnosis.collection;

import com.junsang.festival.domain.diagnosis.RecurrenceType;
import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.ConcentrationAnalysisService;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RegionalVisitorAnalysisService;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisResult;
import com.junsang.festival.domain.diagnosis.analysis.RelatedPlaceAnalysisService;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisDataContext;
import com.junsang.festival.domain.diagnosis.calculation.DiagnosisMetric;
import com.junsang.festival.domain.diagnosis.data.ExternalDataResult;
import com.junsang.festival.domain.diagnosis.entity.Diagnosis;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalDetailResponse;
import com.junsang.festival.domain.festival.service.FestivalService;
import com.junsang.festival.infra.tourapi.client.TourConcentrationClient;
import com.junsang.festival.infra.tourapi.client.TourRegionalVisitorClient;
import com.junsang.festival.infra.tourapi.client.TourRelatedPlaceClient;
import com.junsang.festival.infra.tourapi.config.TourExternalDataProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 외부 데이터 수집·정제
@Component
@RequiredArgsConstructor
public class DefaultDiagnosisDataCollector implements DiagnosisDataCollector {

    private final TourConcentrationClient concentrationClient;
    private final TourRelatedPlaceClient relatedPlaceClient;
    private final TourRegionalVisitorClient regionalVisitorClient;
    private final ConcentrationAnalysisService concentrationAnalysisService;
    private final RelatedPlaceAnalysisService relatedPlaceAnalysisService;
    private final RegionalVisitorAnalysisService regionalVisitorAnalysisService;
    private final FestivalService festivalService;
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

        TargetLocation targetLocation = targetLocation(diagnosis);
        sourceData.put(DiagnosisDataKeys.TARGET_LOCATION, targetLocation);
        ExternalDataResult<CompetingFestivalResponse> competing = competing(diagnosis, targetLocation);
        sourceData.put(DiagnosisDataKeys.COMPETING_FESTIVALS, competing);
        addCompetingMetrics(competing, metrics);

        return new DiagnosisDataContext(diagnosis, Map.copyOf(sourceData), List.copyOf(metrics));
    }

    private ExternalDataResult<ConcentrationAnalysisResult> concentration(Diagnosis diagnosis) {
        LocalDate today = LocalDate.now();
        String period = diagnosis.getStartDate() + "~" + diagnosis.getEndDate();
        if (diagnosis.getStartDate().isBefore(today) || diagnosis.getStartDate().isAfter(today.plusDays(30))) {
            return ExternalDataResult.outOfRange("관광지 집중률은 조회일 기준 향후 30일만 제공합니다.", period);
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
        String period = externalDataProperties.relatedBaseYearMonth();
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

    private TargetLocation targetLocation(Diagnosis diagnosis) {
        if (diagnosis.getRecurrenceType() == RecurrenceType.NEW) {
            return new TargetLocation(diagnosis.getLatitude(), diagnosis.getLongitude(), "USER_INPUT");
        }
        try {
            FestivalDetailResponse festival = festivalService.getFestival(diagnosis.getExistingFestivalContentId());
            return new TargetLocation(
                    new BigDecimal(festival.latitude()),
                    new BigDecimal(festival.longitude()),
                    "KOR_SERVICE"
            );
        } catch (RuntimeException exception) {
            return new TargetLocation(null, null, "UNAVAILABLE");
        }
    }

    private ExternalDataResult<CompetingFestivalResponse> competing(
            Diagnosis diagnosis,
            TargetLocation location
    ) {
        String period = diagnosis.getStartDate() + "~" + diagnosis.getEndDate();
        if (location.latitude() == null || location.longitude() == null) {
            return ExternalDataResult.failed("기준 좌표가 없어 경합 축제 거리를 계산할 수 없습니다.", period);
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
                    ? ExternalDataResult.noData("50km 이내 동기간 경합 축제가 없습니다.", period)
                    : ExternalDataResult.available(period, result);
        } catch (RuntimeException exception) {
            return ExternalDataResult.failed(safeReason(exception), period);
        }
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
        int count = result.data() == null ? 0 : result.data().count();
        String names = result.data() == null ? "" : result.data().festivals().stream()
                .map(festival -> festival.title())
                .collect(java.util.stream.Collectors.joining(", "));
        metrics.add(new DiagnosisMetric(
                "COMPETING_FESTIVAL_COUNT_50KM",
                BigDecimal.valueOf(count),
                Map.of("festivalNames", names)
        ));
    }

    private String safeReason(RuntimeException exception) {
        return exception.getMessage() == null ? "외부 데이터를 처리하지 못했습니다." : exception.getMessage();
    }

    public record TargetLocation(BigDecimal latitude, BigDecimal longitude, String source) {
    }
}
