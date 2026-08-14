package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.dto.concentration.TourConcentrationForecastItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourConcentrationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalizesSingleObjectItemToList() throws Exception {
        TourConcentrationClient client = new TourConcentrationClient(null, null, null);
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {"items": {"item": {
                      "baseYmd": "20260822", "areaCd": "51", "areaNm": "강원특별자치도",
                      "signguCd": "51130", "signguNm": "원주시", "tAtsNm": "구룡사", "cnctrRate": "95.58"
                    }}}
                  }
                }
                """;

        List<TourConcentrationForecastItem> items = client.parseItems(objectMapper.readTree(response));

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.baseDate()).hasToString("2026-08-22");
            assertThat(item.placeName()).isEqualTo("구룡사");
            assertThat(item.concentrationRate()).hasToString("95.58");
        });
    }

    @Test
    void loadsEveryPageUsingTotalCount(@TempDir Path tempDirectory) throws Exception {
        TourApiClient apiClient = mock(TourApiClient.class);
        when(apiClient.request(eq("concentration-forecast"), anyMap())).thenAnswer(invocation -> {
            Map<String, String> parameters = invocation.getArgument(1);
            int pageNo = Integer.parseInt(parameters.get("pageNo"));
            return objectMapper.readTree(responsePage(pageNo));
        });
        TourConcentrationClient client = new TourConcentrationClient(
                apiClient, new FileJsonCache(tempDirectory.toString()), Duration.ofHours(1)
        );

        List<TourConcentrationForecastItem> items = client.getForecastItems("51", "51130");

        assertThat(items).extracting(TourConcentrationForecastItem::placeName)
                .containsExactly("관광지1", "관광지2");
    }

    private String responsePage(int pageNo) {
        String placeName = pageNo == 1 ? "관광지1" : "관광지2";
        return """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {
                      "totalCount": 2,
                      "pageNo": %d,
                      "numOfRows": 1,
                      "items": {"item": [{
                        "baseYmd": "20260822", "areaCd": "51", "areaNm": "강원특별자치도",
                        "signguCd": "51130", "signguNm": "원주시", "tAtsNm": "%s", "cnctrRate": "50"
                      }]}
                    }
                  }
                }
                """.formatted(pageNo, placeName);
    }
}
