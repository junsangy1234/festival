package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.cache.CachedExternalData;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.config.TourExternalDataProperties;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import com.junsang.festival.infra.tourapi.dto.hub.TourHubAttractionItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// API #6 기초지자체중심 관광지정보를 조회한다. 관광지 좌표(mapX·mapY)와 hubRank가 필요한 곳에서 쓴다.
@Component
public class TourHubAttractionClient {

    private static final String SOURCE = "local-hub-attractions";
    private static final int PAGE_SIZE = 1000;

    private final TourApiClient tourApiClient;
    private final FileJsonCache fileJsonCache;
    private final Duration cacheTtl;
    private final TourExternalDataProperties properties;

    public TourHubAttractionClient(
            TourApiClient tourApiClient,
            FileJsonCache fileJsonCache,
            @Value("${festival.cache.hub-attractions-ttl:P30D}") Duration cacheTtl,
            TourExternalDataProperties properties
    ) {
        this.tourApiClient = tourApiClient;
        this.fileJsonCache = fileJsonCache;
        this.cacheTtl = cacheTtl;
        this.properties = properties;
    }

    // 시군구의 중심 관광지를 hubRank 순으로 모두 조회한다.
    public List<TourHubAttractionItem> getAll(String areaCode, String signguCode) {
        List<TourHubAttractionItem> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            CachedExternalData page = getPage(areaCode, signguCode, pageNo);
            List<TourHubAttractionItem> pageItems = parseItems(page.payload());
            result.addAll(pageItems);
            int totalCount = page.payload().path("response").path("body").path("totalCount").asInt(0);
            if (pageItems.isEmpty() || result.size() >= totalCount) {
                return result.stream()
                        .sorted(Comparator.comparing(
                                TourHubAttractionItem::hubRank,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
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
                () -> tourApiClient.request(TourApiOperation.LOCAL_HUB_ATTRACTIONS.key(), parameters)
        );
    }

    // 배열·단일 객체 응답을 중심 관광지 목록으로 정규화한다.
    public List<TourHubAttractionItem> parseItems(JsonNode payload) {
        String code = payload.path("response").path("header").path("resultCode").asText();
        if (!"0000".equals(code) && !"03".equals(code)) {
            throw new TourApiException("기초지자체중심 관광지정보 API 호출에 실패했습니다. resultCode=" + code);
        }
        JsonNode itemNode = payload.path("response").path("body").path("items").path("item");
        if (itemNode.isMissingNode() || itemNode.isNull() || itemNode.isTextual() && itemNode.asText().isBlank()) {
            return List.of();
        }
        List<TourHubAttractionItem> items = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(item -> items.add(toItem(item)));
        } else {
            items.add(toItem(itemNode));
        }
        return List.copyOf(items);
    }

    private TourHubAttractionItem toItem(JsonNode item) {
        return new TourHubAttractionItem(
                item.path("baseYm").asText(),
                item.path("areaCd").asText(),
                item.path("signguCd").asText(),
                item.path("hubTatsCd").asText(),
                item.path("hubTatsNm").asText(),
                item.path("hubCtgryLclsNm").asText(),
                item.path("hubCtgryMclsNm").asText(),
                item.path("hubCtgrySclsNm").asText(),
                optionalInt(item.path("hubRank").asText()),
                optionalDecimal(item.path("mapX").asText()),
                optionalDecimal(item.path("mapY").asText())
        );
    }

    private Integer optionalInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal optionalDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
