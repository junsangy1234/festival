package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.cache.CachedExternalData;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import com.junsang.festival.infra.tourapi.dto.visitor.TourRegionalVisitorItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 기초 지자체 일별 방문자 API를 전체 페이지 조회하고 원본 JSON을 캐시한다.
@Component
public class TourRegionalVisitorClient {

    private static final String SOURCE = "local-region-visitors";
    private static final int PAGE_SIZE = 1000;
    private static final DateTimeFormatter TOUR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final TourApiClient tourApiClient;
    private final FileJsonCache fileJsonCache;
    private final Duration cacheTtl;

    public TourRegionalVisitorClient(
            TourApiClient tourApiClient,
            FileJsonCache fileJsonCache,
            @Value("${festival.cache.regional-visitors-ttl:PT24H}") Duration cacheTtl
    ) {
        this.tourApiClient = tourApiClient;
        this.fileJsonCache = fileJsonCache;
        this.cacheTtl = cacheTtl;
    }

    // 조회 기간의 전체 기초 지자체 데이터를 받은 뒤 요청 시군구만 반환한다.
    public List<TourRegionalVisitorItem> getAll(String signguCode, LocalDate startDate, LocalDate endDate) {
        List<TourRegionalVisitorItem> result = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            CachedExternalData page = getPage(startDate, endDate, pageNo);
            List<TourRegionalVisitorItem> pageItems = parseItems(page.payload());
            pageItems.stream()
                    .filter(item -> signguCode.equals(item.signguCode()))
                    .forEach(result::add);
            int totalCount = totalCount(page.payload());
            int pageRows = pageItemCount(page.payload());
            if (pageRows == 0 || pageNo * PAGE_SIZE >= totalCount) {
                return result.stream()
                        .sorted(java.util.Comparator.comparing(TourRegionalVisitorItem::baseDate)
                                .thenComparing(TourRegionalVisitorItem::visitorTypeCode))
                        .toList();
            }
            pageNo++;
        }
    }

    // 날짜·페이지 조건을 캐시 키에 포함해 한 페이지의 원본 JSON을 조회한다.
    public CachedExternalData getPage(LocalDate startDate, LocalDate endDate, int pageNo) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("startYmd", startDate.format(TOUR_DATE_FORMATTER));
        parameters.put("endYmd", endDate.format(TOUR_DATE_FORMATTER));
        parameters.put("pageNo", String.valueOf(pageNo));
        parameters.put("numOfRows", String.valueOf(PAGE_SIZE));
        return fileJsonCache.getOrLoad(
                SOURCE,
                parameters,
                cacheTtl,
                () -> tourApiClient.request(TourApiOperation.LOCAL_REGION_VISITORS.key(), parameters)
        );
    }

    // 배열·단일 객체 응답을 일별 방문자 목록으로 정규화한다.
    public List<TourRegionalVisitorItem> parseItems(JsonNode payload) {
        validateResult(payload);
        JsonNode itemNode = itemNode(payload);
        if (isEmpty(itemNode)) {
            return List.of();
        }
        List<TourRegionalVisitorItem> items = new ArrayList<>();
        if (itemNode.isArray()) {
            itemNode.forEach(item -> items.add(toItem(item)));
        } else {
            items.add(toItem(itemNode));
        }
        return List.copyOf(items);
    }

    private TourRegionalVisitorItem toItem(JsonNode item) {
        return new TourRegionalVisitorItem(
                requiredText(item, "signguCode"),
                requiredText(item, "signguNm"),
                requiredText(item, "daywkDivCd"),
                requiredText(item, "daywkDivNm"),
                requiredText(item, "touDivCd"),
                requiredText(item, "touDivNm"),
                new BigDecimal(requiredText(item, "touNum")),
                LocalDate.parse(requiredText(item, "baseYmd"), TOUR_DATE_FORMATTER)
        );
    }

    private int totalCount(JsonNode payload) {
        return payload.path("response").path("body").path("totalCount").asInt(0);
    }

    private int pageItemCount(JsonNode payload) {
        JsonNode node = itemNode(payload);
        if (isEmpty(node)) {
            return 0;
        }
        return node.isArray() ? node.size() : 1;
    }

    private JsonNode itemNode(JsonNode payload) {
        return payload.path("response").path("body").path("items").path("item");
    }

    private boolean isEmpty(JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.isTextual() && node.asText().isBlank();
    }

    private void validateResult(JsonNode payload) {
        String code = payload.path("response").path("header").path("resultCode").asText();
        if (!"0000".equals(code) && !"03".equals(code)) {
            throw new TourApiException("기초 지자체 방문자 API 호출에 실패했습니다. resultCode=" + code);
        }
    }

    private String requiredText(JsonNode item, String fieldName) {
        String value = item.path(fieldName).asText();
        if (value.isBlank()) {
            throw new TourApiException("기초 지자체 방문자 응답에 " + fieldName + " 필드가 없습니다.");
        }
        return value;
    }
}
