package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.cache.CachedExternalData;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import com.junsang.festival.infra.tourapi.dto.concentration.TourConcentrationForecastItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 관광지 집중률 예측 API를 호출하고 일 단위 JSON 캐시를 적용한다.
@Component
public class TourConcentrationClient {

    private static final String SOURCE = "concentration-forecast";
    private static final int PAGE_SIZE = 1000;

    private final TourApiClient tourApiClient;
    private final FileJsonCache fileJsonCache;
    private final Duration concentrationForecastTtl;

    public TourConcentrationClient(
            TourApiClient tourApiClient,
            FileJsonCache fileJsonCache,
            @Value("${festival.cache.concentration-forecast-ttl:PT24H}") Duration concentrationForecastTtl
    ) {
        this.tourApiClient = tourApiClient;
        this.fileJsonCache = fileJsonCache;
        this.concentrationForecastTtl = concentrationForecastTtl;
    }

    // 시군구 전체 관광지의 향후 26일(D+0~D+25) 집중률 예측 원본 데이터를 조회한다.
    public CachedExternalData getForecast(String areaCode, String signguCode) {
        return getForecastPage(areaCode, signguCode, 1);
    }

    // 지역·페이지 조건을 모두 캐시 키에 포함해 한 페이지를 조회한다.
    public CachedExternalData getForecastPage(String areaCode, String signguCode, int pageNo) {
        Map<String, String> parameters = Map.of(
                "areaCd", areaCode,
                "signguCd", signguCode,
                "pageNo", String.valueOf(pageNo),
                "numOfRows", String.valueOf(PAGE_SIZE)
        );
        return fileJsonCache.getOrLoad(
                SOURCE,
                parameters,
                concentrationForecastTtl,
                () -> tourApiClient.request(TourApiOperation.CONCENTRATION_FORECAST.key(), parameters)
        );
    }

    // 원본 응답의 배열·객체 이중 구조를 정규화해 집중률 예측 목록으로 반환한다.
    public List<TourConcentrationForecastItem> getForecastItems(String areaCode, String signguCode) {
        List<TourConcentrationForecastItem> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            JsonNode payload = getForecastPage(areaCode, signguCode, pageNo).payload();
            List<TourConcentrationForecastItem> pageItems = parseItems(payload);
            result.addAll(pageItems);
            int totalCount = payload.path("response").path("body").path("totalCount").asInt(0);
            if (pageItems.isEmpty() || result.size() >= totalCount) {
                return List.copyOf(result);
            }
            pageNo++;
        }
    }

    // 관광공사 성공 코드와 items.item 구조를 확인한 뒤 서비스에서 사용할 타입으로 변환한다.
    public List<TourConcentrationForecastItem> parseItems(JsonNode payload) {
        String resultCode = payload.path("response").path("header").path("resultCode").asText();
        if ("03".equals(resultCode)) {
            return List.of();
        }
        if (!"0000".equals(resultCode)) {
            throw new TourApiException("관광지 집중률 예측 API 호출에 실패했습니다. resultCode=" + resultCode);
        }

        JsonNode itemNode = payload.path("response").path("body").path("items").path("item");
        if (itemNode.isMissingNode() || itemNode.isNull() || itemNode.isTextual() && itemNode.asText().isBlank()) {
            return List.of();
        }

        List<TourConcentrationForecastItem> items = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(item -> items.add(toItem(item)));
        } else {
            items.add(toItem(itemNode));
        }
        return List.copyOf(items);
    }

    // 문자열로 내려오는 날짜와 집중률을 LocalDate·BigDecimal로 변환한다.
    private TourConcentrationForecastItem toItem(JsonNode item) {
        return new TourConcentrationForecastItem(
                LocalDate.parse(requiredText(item, "baseYmd"), DateTimeFormatter.BASIC_ISO_DATE),
                requiredText(item, "areaCd"),
                requiredText(item, "areaNm"),
                requiredText(item, "signguCd"),
                requiredText(item, "signguNm"),
                requiredText(item, "tAtsNm"),
                new BigDecimal(requiredText(item, "cnctrRate"))
        );
    }

    // 필수 응답 필드가 누락되면 잘못된 원본 응답으로 처리한다.
    private String requiredText(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asText();
        if (value.isBlank()) {
            throw new TourApiException("관광지 집중률 예측 응답에 " + fieldName + " 필드가 없습니다.");
        }
        return value;
    }
}
