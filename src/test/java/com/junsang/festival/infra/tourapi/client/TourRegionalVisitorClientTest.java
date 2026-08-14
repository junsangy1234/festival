package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsang.festival.infra.tourapi.dto.visitor.TourRegionalVisitorItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourRegionalVisitorClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesRegionalVisitorResponse() throws Exception {
        TourRegionalVisitorClient client = new TourRegionalVisitorClient(null, null, null);
        String response = """
                {
                  "response": {
                    "header": {"resultCode": "0000"},
                    "body": {"items": {"item": {
                      "signguCode": "11110", "signguNm": "종로구",
                      "daywkDivCd": "4", "daywkDivNm": "목요일",
                      "touDivCd": "1", "touDivNm": "현지인(a)",
                      "touNum": "176473.5", "baseYmd": "20210513"
                    }}}
                  }
                }
                """;

        List<TourRegionalVisitorItem> items = client.parseItems(objectMapper.readTree(response));

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.signguName()).isEqualTo("종로구");
            assertThat(item.visitorCount()).isEqualByComparingTo("176473.5");
            assertThat(item.baseDate()).hasToString("2021-05-13");
        });
    }
}
