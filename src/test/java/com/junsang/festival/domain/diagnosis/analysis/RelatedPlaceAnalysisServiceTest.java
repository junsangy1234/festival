package com.junsang.festival.domain.diagnosis.analysis;

import com.junsang.festival.infra.tourapi.dto.related.TourRelatedPlaceItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedPlaceAnalysisServiceTest {

    private final RelatedPlaceAnalysisService service = new RelatedPlaceAnalysisService();

    @Test
    void groupsByBasePlaceAndSortsRelatedPlacesByRank() {
        List<TourRelatedPlaceItem> items = List.of(
                place("A", "기준 관광지", "R2", "두 번째", "자연", "산", "계곡", 2),
                place("A", "기준 관광지", "R1", "첫 번째", "문화", "역사", "사찰", 1)
        );

        RelatedPlaceAnalysisResult result = service.analyze(items);

        assertThat(result.basePlaces()).singleElement().satisfies(base -> {
            assertThat(base.placeCode()).isEqualTo("A");
            assertThat(base.relatedPlaces()).extracting("rank").containsExactly(1, 2);
        });
        assertThat(result.averageTopTenRank()).isEqualByComparingTo("1.50");
        assertThat(result.categoryDiversity()).isEqualTo(2);
    }

    private TourRelatedPlaceItem place(
            String baseCode,
            String baseName,
            String relatedCode,
            String relatedName,
            String large,
            String medium,
            String small,
            int rank
    ) {
        return new TourRelatedPlaceItem(
                "202503", baseCode, baseName, "51", "강원", "51130", "원주시",
                relatedCode, relatedName, "51", "강원", "51130", "원주시",
                large, medium, small, rank
        );
    }
}
