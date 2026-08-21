package com.junsang.festival.domain.diagnosis.collection;

// MVP 데이터 스택(API #1·#2·#6·#7·#8 + CSV #9)의 수집 결과 키다.
public final class DiagnosisDataKeys {

    // API #1 관광지 집중률 예측
    public static final String CONCENTRATION = "concentration";
    // API #7 빅데이터 지역별 방문자수
    public static final String REGIONAL_VISITORS = "regionalVisitors";
    // API #2 관광지별 연관 관광지
    public static final String RELATED_PLACES = "relatedPlaces";
    // API #6 기초지자체중심 관광지정보(좌표·hubRank)
    public static final String HUB_ATTRACTIONS = "hubAttractions";
    // API #8 국문 관광정보(동기간 인근 축제)
    public static final String COMPETING_FESTIVALS = "competingFestivals";
    // CSV #9 문체부 지역축제 개최 계획 현황(재개최 실적)
    public static final String FESTIVAL_HISTORY = "festivalHistory";
    // 축제장 위치(기획서 Part 5.5)
    public static final String FESTIVAL_LOCATION = "festivalLocation";

    private DiagnosisDataKeys() {
    }
}
