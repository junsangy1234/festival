package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsang.festival.infra.tourapi.config.TourExternalDataProperties;
import com.junsang.festival.infra.tourapi.dto.related.TourRelatedPlaceItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourRelatedPlaceClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesRelatedPlaceResponse() throws Exception {
        TourRelatedPlaceClient client = new TourRelatedPlaceClient(
                null, null, null, new TourExternalDataProperties("202503", 1)
        );
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {"items": {"item": {
                      "baseYm": "202503", "tAtsCd": "base-1", "tAtsNm": "가리봉시장",
                      "areaCd": "11", "areaNm": "서울특별시", "signguCd": "11530", "signguNm": "구로구",
                      "rlteTatsCd": "related-1", "rlteTatsNm": "서울드래곤시티",
                      "rlteRegnCd": "11", "rlteRegnNm": "서울특별시",
                      "rlteSignguCd": "11170", "rlteSignguNm": "용산구",
                      "rlteCtgryLclsNm": "숙박", "rlteCtgryMclsNm": "숙박",
                      "rlteCtgrySclsNm": "호텔", "rlteRank": "1"
                    }}}
                  }
                }
                """;

        List<TourRelatedPlaceItem> items = client.parseItems(objectMapper.readTree(response));

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.basePlaceName()).isEqualTo("가리봉시장");
            assertThat(item.relatedPlaceName()).isEqualTo("서울드래곤시티");
            assertThat(item.rank()).isEqualTo(1);
        });
    }
}
