package com.junsang.festival.domain.festival.service;

import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.infra.tourapi.client.TourFestivalClient;
import com.junsang.festival.infra.tourapi.dto.festival.TourApiBody;
import com.junsang.festival.infra.tourapi.dto.festival.TourApiEnvelope;
import com.junsang.festival.infra.tourapi.dto.festival.TourApiItems;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FestivalServiceTest {

    @Test
    void filtersCompetingFestivalsByPeriodCoordinatesAndFiftyKilometersForNewFestival() {
        TourFestivalClient client = mock(TourFestivalClient.class);
        FestivalService service = new FestivalService(client, new GeoDistanceService());
        List<TourFestivalSearchItem> items = List.of(
                festival("inside", "20260820", "20260822", "127.0000", "37.4490"),
                festival("outside", "20260820", "20260822", "127.0000", "37.5000"),
                festival("missing", "20260820", "20260822", null, null),
                festival("other-period", "20260901", "20260902", "127.0000", "37.0100")
        );
        when(client.searchFestivals(anyMap())).thenReturn(response(items));

        CompetingFestivalResponse result = service.findCompetingFestivals(
                null,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 23),
                new BigDecimal("37.0000"),
                new BigDecimal("127.0000")
        );

        assertThat(result.festivals()).extracting(festival -> festival.contentId()).containsExactly("inside");
        assertThat(result.festivals().getFirst().distanceKm()).isEqualByComparingTo("49.9");
        assertThat(result.excludedMissingCoordinatesCount()).isEqualTo(1);
    }

    private TourFestivalSearchApiResponse response(List<TourFestivalSearchItem> items) {
        return new TourFestivalSearchApiResponse(new TourApiEnvelope<>(new TourApiBody<>(
                new TourApiItems<>(items), items.size(), 1, items.size()
        )));
    }

    private TourFestivalSearchItem festival(
            String contentId,
            String startDate,
            String endDate,
            String longitude,
            String latitude
    ) {
        return new TourFestivalSearchItem(
                contentId, "15", contentId, startDate, endDate,
                "32", "230", "32", "230",
                "주소", null, longitude, latitude, null, null, null
        );
    }
}
