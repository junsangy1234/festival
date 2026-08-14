package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.concentration.TourConcentrationForecastItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// 집중률 분석
@Service
public class ConcentrationAnalysisService {

    private static final BigDecimal WARNING_THRESHOLD = BigDecimal.TEN;
    private static final BigDecimal SURGING_THRESHOLD = BigDecimal.valueOf(20);
    private static final BigDecimal RELAXED_DELTA_THRESHOLD = BigDecimal.valueOf(-5);
    private static final BigDecimal RELAXED_RATE_THRESHOLD = BigDecimal.valueOf(40);

    // 30일 원본에서 개최기간 평균, 관광지별 상승폭과 여유 후보를 함께 만든다.
    public ConcentrationAnalysisResult analyze(
            List<TourConcentrationForecastItem> forecastItems,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validatePeriod(startDate, endDate);
        Map<String, List<TourConcentrationForecastItem>> itemsByPlace = forecastItems.stream()
                .collect(Collectors.groupingBy(
                        TourConcentrationForecastItem::placeName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<String, BigDecimal> thirtyDayAverages = itemsByPlace.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> average(entry.getValue().stream()
                                .map(TourConcentrationForecastItem::concentrationRate)
                                .toList()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<TourConcentrationForecastItem> festivalItems = forecastItems.stream()
                .filter(item -> isFestivalDate(item.baseDate(), startDate, endDate))
                .toList();

        return new ConcentrationAnalysisResult(
                averageOrNull(festivalItems.stream().map(TourConcentrationForecastItem::concentrationRate).toList()),
                dailyConcentrations(festivalItems, thirtyDayAverages),
                volatilityPlaces(itemsByPlace, thirtyDayAverages, startDate, endDate),
                relaxedCandidates(itemsByPlace, thirtyDayAverages, startDate, endDate)
        );
    }

    // View 01에 사용할 개최기간 날짜별 관광지 집중률을 만든다.
    private List<ConcentrationAnalysisResult.DailyConcentration> dailyConcentrations(
            List<TourConcentrationForecastItem> festivalItems,
            Map<String, BigDecimal> thirtyDayAverages
    ) {
        return festivalItems.stream()
                .collect(Collectors.groupingBy(
                        TourConcentrationForecastItem::baseDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ConcentrationAnalysisResult.DailyConcentration(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(TourConcentrationForecastItem::placeName))
                                .map(item -> new ConcentrationAnalysisResult.PlaceRate(
                                        item.placeName(),
                                        item.concentrationRate(),
                                        subtract(item.concentrationRate(), thirtyDayAverages.get(item.placeName()))
                                ))
                                .toList()
                ))
                .toList();
    }

    // View 02에 사용할 관광지별 개최기간 최고 상승폭과 배지를 만든다.
    private List<ConcentrationAnalysisResult.VolatilityPlace> volatilityPlaces(
            Map<String, List<TourConcentrationForecastItem>> itemsByPlace,
            Map<String, BigDecimal> thirtyDayAverages,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<ConcentrationAnalysisResult.VolatilityPlace> results = new ArrayList<>();
        itemsByPlace.forEach((placeName, placeItems) -> placeItems.stream()
                .filter(item -> isFestivalDate(item.baseDate(), startDate, endDate))
                .max(Comparator.comparing(TourConcentrationForecastItem::concentrationRate))
                .ifPresent(peak -> {
                    BigDecimal peakDelta = subtract(peak.concentrationRate(), thirtyDayAverages.get(placeName));
                    results.add(new ConcentrationAnalysisResult.VolatilityPlace(
                            placeName,
                            thirtyDayAverages.get(placeName),
                            peak.concentrationRate(),
                            peakDelta,
                            peak.baseDate(),
                            badgeOf(peakDelta)
                    ));
                }));
        return results.stream()
                .sorted(Comparator.comparing(ConcentrationAnalysisResult.VolatilityPlace::peakDelta).reversed()
                        .thenComparing(ConcentrationAnalysisResult.VolatilityPlace::placeName))
                .toList();
    }

    // 여유 관광지 선별
    private List<ConcentrationAnalysisResult.RelaxedPlaceCandidate> relaxedCandidates(
            Map<String, List<TourConcentrationForecastItem>> itemsByPlace,
            Map<String, BigDecimal> thirtyDayAverages,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<ConcentrationAnalysisResult.RelaxedPlaceCandidate> results = new ArrayList<>();
        itemsByPlace.forEach((placeName, placeItems) -> {
            BigDecimal festivalAverage = averageOrNull(placeItems.stream()
                    .filter(item -> isFestivalDate(item.baseDate(), startDate, endDate))
                    .map(TourConcentrationForecastItem::concentrationRate)
                    .toList());
            if (festivalAverage == null) {
                return;
            }

            BigDecimal delta = subtract(festivalAverage, thirtyDayAverages.get(placeName));
            if (delta.compareTo(RELAXED_DELTA_THRESHOLD) <= 0
                    && festivalAverage.compareTo(RELAXED_RATE_THRESHOLD) <= 0) {
                results.add(new ConcentrationAnalysisResult.RelaxedPlaceCandidate(
                        placeName,
                        thirtyDayAverages.get(placeName),
                        festivalAverage,
                        delta
                ));
            }
        });
        return results.stream()
                .sorted(Comparator.comparing(ConcentrationAnalysisResult.RelaxedPlaceCandidate::deltaFromThirtyDayAverage)
                        .thenComparing(ConcentrationAnalysisResult.RelaxedPlaceCandidate::placeName))
                .toList();
    }

    private ConcentrationBadge badgeOf(BigDecimal peakDelta) {
        if (peakDelta.compareTo(SURGING_THRESHOLD) > 0) {
            return ConcentrationBadge.SURGING;
        }
        if (peakDelta.compareTo(WARNING_THRESHOLD) > 0) {
            return ConcentrationBadge.WARNING;
        }
        return ConcentrationBadge.STABLE;
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("개최기간이 올바르지 않습니다.");
        }
    }

    private boolean isFestivalDate(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private BigDecimal average(List<BigDecimal> values) {
        return averageOrNull(values);
    }

    private BigDecimal averageOrNull(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left.subtract(right).setScale(2, RoundingMode.HALF_UP);
    }
}
