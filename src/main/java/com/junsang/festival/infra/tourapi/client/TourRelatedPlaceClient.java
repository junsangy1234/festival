package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.cache.CachedExternalData;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.config.TourExternalDataProperties;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import com.junsang.festival.infra.tourapi.dto.related.TourRelatedPlaceItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 관광지별 연관 관광지 API를 전체 페이지 조회하고 원본 JSON을 캐시한다.
@Component
public class TourRelatedPlaceClient {

    private static final String SOURCE = "related-tourist-places";
    private static final int PAGE_SIZE = 1000;

    private final TourApiClient tourApiClient;
    private final FileJsonCache fileJsonCache;
    private final Duration cacheTtl;
    private final TourExternalDataProperties properties;

    public TourRelatedPlaceClient(
            TourApiClient tourApiClient,
            FileJsonCache fileJsonCache,
            @Value("${festival.cache.related-places-ttl:P30D}") Duration cacheTtl,
            TourExternalDataProperties properties
    ) {
        this.tourApiClient = tourApiClient;
        this.fileJsonCache = fileJsonCache;
        this.cacheTtl = cacheTtl;
        this.properties = properties;
    }

    // 설정된 기준 연월과 개최 지역의 연관 관광지를 모두 조회한다.
    public List<TourRelatedPlaceItem> getAll(String areaCode, String signguCode) {
        List<TourRelatedPlaceItem> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            CachedExternalData page = getPage(areaCode, signguCode, pageNo);
            List<TourRelatedPlaceItem> pageItems = parseItems(page.payload());
            result.addAll(pageItems);
            int totalCount = totalCount(page.payload());
            if (pageItems.isEmpty() || result.size() >= totalCount) {
                return result.stream()
                        .sorted(Comparator.comparing(TourRelatedPlaceItem::basePlaceCode)
                                .thenComparingInt(TourRelatedPlaceItem::rank))
                        .toList();
            }
            pageNo++;
        }
    }

    // 실제 요청 조건을 캐시 키에 포함해 한 페이지의 원본 JSON을 조회한다.
    public CachedExternalData getPage(String areaCode, String signguCode, int pageNo) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("baseYm", properties.resolvedRelatedBaseYearMonth());
        parameters.put("areaCd", areaCode);
        parameters.put("signguCd", signguCode);
        parameters.put("pageNo", String.valueOf(pageNo));
        parameters.put("numOfRows", String.valueOf(PAGE_SIZE));
        return fileJsonCache.getOrLoad(
                SOURCE,
                parameters,
                cacheTtl,
                () -> tourApiClient.request(TourApiOperation.RELATED_TOURIST_PLACES.key(), parameters)
        );
    }

    // 배열·단일 객체 응답을 연관 관광지 목록으로 정규화한다.
    public List<TourRelatedPlaceItem> parseItems(JsonNode payload) {
        validateResult(payload, "연관 관광지");
        JsonNode itemNode = itemNode(payload);
        if (isEmpty(itemNode)) {
            return List.of();
        }
        List<TourRelatedPlaceItem> items = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(item -> items.add(toItem(item)));
        } else {
            items.add(toItem(itemNode));
        }
        return List.copyOf(items);
    }

    private TourRelatedPlaceItem toItem(JsonNode item) {
        return new TourRelatedPlaceItem(
                requiredText(item, "baseYm"),
                requiredText(item, "tAtsCd"),
                requiredText(item, "tAtsNm"),
                requiredText(item, "areaCd"),
                item.path("areaNm").asText(),
                requiredText(item, "signguCd"),
                item.path("signguNm").asText(),
                requiredText(item, "rlteTatsCd"),
                requiredText(item, "rlteTatsNm"),
                item.path("rlteRegnCd").asText(),
                item.path("rlteRegnNm").asText(),
                item.path("rlteSignguCd").asText(),
                item.path("rlteSignguNm").asText(),
                item.path("rlteCtgryLclsNm").asText(),
                item.path("rlteCtgryMclsNm").asText(),
                item.path("rlteCtgrySclsNm").asText(),
                Integer.parseInt(requiredText(item, "rlteRank"))
        );
    }

    private int totalCount(JsonNode payload) {
        return payload.path("response").path("body").path("totalCount").asInt(0);
    }

    private JsonNode itemNode(JsonNode payload) {
        return payload.path("response").path("body").path("items").path("item");
    }

    private boolean isEmpty(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.isTextual() && node.asText().isBlank();
    }

    private void validateResult(JsonNode payload, String apiName) {
        String code = payload.path("response").path("header").path("resultCode").asText();
        if (!"0000".equals(code) && !"03".equals(code)) {
            throw new TourApiException(apiName + " API 호출에 실패했습니다. resultCode=" + code);
        }
    }

    private String requiredText(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asText();
        if (value.isBlank()) {
            throw new TourApiException("연관 관광지 응답에 " + fieldName + " 필드가 없습니다.");
        }
        return value;
    }
}
