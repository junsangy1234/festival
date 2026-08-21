package com.junsang.festival.infra.tourapi.dto;

import com.junsang.festival.global.exception.TourApiException;

import java.util.Arrays;

public enum TourApiOperation {
    CONCENTRATION_FORECAST("concentration-forecast", "/B551011/TatsCnctrRateService/tatsCnctrRatedList"),
    RELATED_TOURIST_PLACES("related-tourist-places", "/B551011/TarRlteTarService1/areaBasedList1"),
    METRO_REGION_VISITORS("metro-region-visitors", "/B551011/DataLabService/metcoRegnVisitrDDList"),
    LOCAL_REGION_VISITORS("local-region-visitors", "/B551011/DataLabService/locgoRegnVisitrDDList"),
    // API #6 기초지자체중심 관광지정보. v7.3에서 MVP 편입(뷰 03 강화·R-VOL-005 좌표 정밀화).
    LOCAL_HUB_ATTRACTIONS("local-hub-attractions", "/B551011/LocgoHubTarService1/areaBasedList1"),
    // API #3·#4·#5(지역별 관광자원 수요·관광수요 강도·관광다양성)는 v7.4에서 완전 폐기했다.
    // 인증·파라미터가 유효해도 모든 조합에서 totalCount=0을 반환해 서비스에서 제외한다.
    AREA_BASED_CONTENTS("area-based-contents", "/B551011/KorService2/areaBasedList2"),
    LOCATION_BASED_CONTENTS("location-based-contents", "/B551011/KorService2/locationBasedList2"),
    KEYWORD_SEARCH("keyword-search", "/B551011/KorService2/searchKeyword2"),
    FESTIVAL_SEARCH("festival-search", "/B551011/KorService2/searchFestival2"),
    STAY_SEARCH("stay-search", "/B551011/KorService2/searchStay2"),
    CONTENT_COMMON("content-common", "/B551011/KorService2/detailCommon2"),
    CONTENT_INTRO("content-intro", "/B551011/KorService2/detailIntro2"),
    CONTENT_DETAIL_INFO("content-detail-info", "/B551011/KorService2/detailInfo2"),
    CONTENT_IMAGES("content-images", "/B551011/KorService2/detailImage2"),
    CONTENT_SYNC("content-sync", "/B551011/KorService2/areaBasedSyncList2"),
    PET_TOUR("pet-tour", "/B551011/KorService2/detailPetTour2"),
    LEGAL_DONG_CODES("legal-dong-codes", "/B551011/KorService2/ldongCode2"),
    CATEGORY_CODES("category-codes", "/B551011/KorService2/lclsSystmCode2");

    private final String key;
    private final String path;

    // 개발용 작업 키와 실제 관광공사 API 경로를 초기화한다.
    TourApiOperation(String key, String path) {
        this.key = key;
        this.path = path;
    }

    // 개발용·내부 호출에 사용할 작업 키를 반환한다.
    public String key() {
        return key;
    }

    // 관광공사에 요청할 API 경로를 반환한다.
    public String path() {
        return path;
    }

    // 작업 키에 대응하는 관광공사 API 작업 enum을 찾는다.
    public static TourApiOperation fromKey(String key) {
        return Arrays.stream(values())
                .filter(operation -> operation.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new TourApiException("지원하지 않는 TourAPI 작업입니다: " + key));
    }
}
