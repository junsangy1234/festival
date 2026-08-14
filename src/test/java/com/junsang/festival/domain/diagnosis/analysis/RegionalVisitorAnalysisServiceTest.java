package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.visitor.TourRegionalVisitorItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegionalVisitorAnalysisServiceTest {

    private final RegionalVisitorAnalysisService service = new RegionalVisitorAnalysisService();

    @Test
    void aggregatesVisitorTypesAndComparesFestivalPeriodWithPreviousPeriod() {
        LocalDate festivalDate = LocalDate.of(2025, 8, 20);
        List<TourRegionalVisitorItem> items = List.of(
                visitor(festivalDate.minusDays(1), "1", 40),
                visitor(festivalDate.minusDays(1), "2", 60),
                visitor(festivalDate, "1", 100),
                visitor(festivalDate, "2", 50),
                visitor(festivalDate, "3", 10),
                visitor(festivalDate.plusDays(1), "1", 80)
        );

        RegionalVisitorAnalysisResult result = service.analyze(items, festivalDate, festivalDate);

        assertThat(result.dailyVisitors()).hasSize(3);
        assertThat(result.festivalPeriodAverage()).isEqualByComparingTo("160.00");
        assertThat(result.beforePeriodAverage()).isEqualByComparingTo("100.00");
        assertThat(result.afterPeriodAverage()).isEqualByComparingTo("80.00");
        assertThat(result.changeFromBeforePercent()).isEqualByComparingTo("60.00");
    }

    private TourRegionalVisitorItem visitor(LocalDate date, String typeCode, long count) {
        return new TourRegionalVisitorItem(
                "51130", "원주시", "", "", typeCode, "", BigDecimal.valueOf(count), date
        );
    }
}
