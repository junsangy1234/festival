package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.visitor.TourRegionalVisitorItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 지역 방문자 분석
@Service
public class RegionalVisitorAnalysisService {

    // 기간 전후 방문자 분석
    public RegionalVisitorAnalysisResult analyze(
            List<TourRegionalVisitorItem> items,
            LocalDate referenceStartDate,
            LocalDate referenceEndDate
    ) {
        Map<LocalDate, List<TourRegionalVisitorItem>> byDate = items.stream()
                .collect(Collectors.groupingBy(TourRegionalVisitorItem::baseDate));
        List<RegionalVisitorAnalysisResult.DailyVisitor> daily = byDate.entrySet().stream()
                .map(entry -> daily(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RegionalVisitorAnalysisResult.DailyVisitor::date))
                .toList();

        BigDecimal festivalAverage = average(daily.stream()
                .filter(item -> within(item.date(), referenceStartDate, referenceEndDate))
                .map(RegionalVisitorAnalysisResult.DailyVisitor::totalVisitors)
                .toList());
        BigDecimal beforeAverage = average(daily.stream()
                .filter(item -> within(item.date(), referenceStartDate.minusDays(7), referenceStartDate.minusDays(1)))
                .map(RegionalVisitorAnalysisResult.DailyVisitor::totalVisitors)
                .toList());
        BigDecimal afterAverage = average(daily.stream()
                .filter(item -> within(item.date(), referenceEndDate.plusDays(1), referenceEndDate.plusDays(7)))
                .map(RegionalVisitorAnalysisResult.DailyVisitor::totalVisitors)
                .toList());

        return new RegionalVisitorAnalysisResult(
                referenceStartDate,
                referenceEndDate,
                daily,
                festivalAverage,
                beforeAverage,
                afterAverage,
                percentChange(festivalAverage, beforeAverage)
        );
    }

    private RegionalVisitorAnalysisResult.DailyVisitor daily(
            LocalDate date,
            List<TourRegionalVisitorItem> items
    ) {
        BigDecimal local = count(items, "1");
        BigDecimal outside = count(items, "2");
        BigDecimal foreign = count(items, "3");
        return new RegionalVisitorAnalysisResult.DailyVisitor(
                date, local, outside, foreign, local.add(outside).add(foreign)
        );
    }

    private BigDecimal count(List<TourRegionalVisitorItem> items, String typeCode) {
        return items.stream()
                .filter(item -> typeCode.equals(item.visitorTypeCode()))
                .map(TourRegionalVisitorItem::visitorCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean within(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
