package com.junsang.festival.infra.tourapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junsang.festival.global.exception.TourApiException;
import com.junsang.festival.infra.cache.FileJsonCache;
import com.junsang.festival.infra.tourapi.dto.TourApiOperation;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalCommonApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalDetailInfoApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalImageApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalIntroApiResponse;
import com.junsang.festival.infra.tourapi.dto.festival.TourFestivalSearchApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

// 축제 검색과 축제 콘텐츠 상세 조회에 필요한 관광공사 API를 담당한다.
@Component
public class TourFestivalClient {

    private static final String FESTIVAL_SEARCH_SOURCE = "festival-search";

    private final TourApiClient tourApiClient;
    private final FileJsonCache fileJsonCache;
    private final Duration festivalSearchTtl;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public TourFestivalClient(
            TourApiClient tourApiClient,
            FileJsonCache fileJsonCache,
            @Value("${festival.cache.festival-search-ttl:PT1H}") Duration festivalSearchTtl
    ) {
        this.tourApiClient = tourApiClient;
        this.fileJsonCache = fileJsonCache;
        this.festivalSearchTtl = festivalSearchTtl;
    }

    // 기간과 지역 조건으로 축제 목록을 DTO 형태로 조회한다.
    public TourFestivalSearchApiResponse searchFestivals(Map<String, String> parameters) {
        var cached = fileJsonCache.getOrLoad(
                FESTIVAL_SEARCH_SOURCE,
                parameters,
                festivalSearchTtl,
                () -> tourApiClient.request(TourApiOperation.FESTIVAL_SEARCH.key(), parameters)
        );
        try {
            return objectMapper.treeToValue(cached.payload(), TourFestivalSearchApiResponse.class);
        } catch (Exception exception) {
            throw new TourApiException("축제 검색 API 응답을 처리하지 못했습니다.", exception);
        }
    }

    // 콘텐츠의 공통 기본 정보를 DTO 형태로 조회한다.
    public TourFestivalCommonApiResponse getContentCommon(Map<String, String> parameters) {
        return tourApiClient.request(
                TourApiOperation.CONTENT_COMMON.key(), parameters, TourFestivalCommonApiResponse.class
        );
    }

    // 축제 콘텐츠의 전용 소개 정보를 DTO 형태로 조회한다.
    public TourFestivalIntroApiResponse getContentIntro(Map<String, String> parameters) {
        return tourApiClient.request(
                TourApiOperation.CONTENT_INTRO.key(), parameters, TourFestivalIntroApiResponse.class
        );
    }

    // 축제 콘텐츠의 상세 안내 항목을 DTO 형태로 조회한다.
    public TourFestivalDetailInfoApiResponse getContentDetailInfo(Map<String, String> parameters) {
        return tourApiClient.request(
                TourApiOperation.CONTENT_DETAIL_INFO.key(), parameters, TourFestivalDetailInfoApiResponse.class
        );
    }

    // 축제 콘텐츠의 대표·추가 이미지를 DTO 형태로 조회한다.
    public TourFestivalImageApiResponse getContentImages(Map<String, String> parameters) {
        return tourApiClient.request(
                TourApiOperation.CONTENT_IMAGES.key(), parameters, TourFestivalImageApiResponse.class
        );
    }
}
