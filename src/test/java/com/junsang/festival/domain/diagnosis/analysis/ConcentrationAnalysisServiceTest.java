package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.concentration.TourConcentrationForecastItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConcentrationAnalysisServiceTest {

    private final ConcentrationAnalysisService service = new ConcentrationAnalysisService();

    @Test
    void createsFestivalTrendVolatilityAndRelaxedCandidates() {
        List<TourConcentrationForecastItem> items = List.of(
                item("구룡사", "20260820", "30"),
                item("구룡사", "20260821", "40"),
                item("구룡사", "20260822", "80"),
                item("구룡사", "20260823", "90"),
                item("여유산", "20260820", "30"),
                item("여유산", "20260821", "30"),
                item("여유산", "20260822", "20"),
                item("여유산", "20260823", "20")
        );

        ConcentrationAnalysisResult result = service.analyze(
                items,
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 8, 23)
        );

        assertThat(result.festivalPeriodAverage()).hasToString("52.50");
        assertThat(result.dailyConcentrations()).hasSize(2);
        assertThat(result.volatilityPlaces()).first().satisfies(place -> {
            assertThat(place.placeName()).isEqualTo("구룡사");
            assertThat(place.peakDelta()).hasToString("30.00");
            assertThat(place.badge()).isEqualTo(ConcentrationBadge.SURGING);
        });
        assertThat(result.relaxedPlaceCandidates()).singleElement().satisfies(place -> {
            assertThat(place.placeName()).isEqualTo("여유산");
            assertThat(place.deltaFromThirtyDayAverage()).hasToString("-5.00");
        });
    }

    private TourConcentrationForecastItem item(String placeName, String date, String concentrationRate) {
        return new TourConcentrationForecastItem(
                LocalDate.parse(date, java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                "51", "강원특별자치도", "51130", "원주시", placeName, new BigDecimal(concentrationRate)
        );
    }
}
