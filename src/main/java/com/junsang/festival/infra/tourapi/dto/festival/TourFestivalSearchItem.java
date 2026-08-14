package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourFestivalSearchItem(
        @JsonProperty("contentid") String contentId,
        @JsonProperty("contenttypeid") String contentTypeId,
        String title,
        @JsonProperty("eventstartdate") String eventStartDate,
        @JsonProperty("eventenddate") String eventEndDate,
        @JsonProperty("areacode") String areaCode,
        @JsonProperty("sigungucode") String sigunguCode,
        @JsonProperty("lDongRegnCd") String legalDongRegionCode,
        @JsonProperty("lDongSignguCd") String legalDongSignguCode,
        @JsonProperty("addr1") String address,
        @JsonProperty("addr2") String addressDetail,
        @JsonProperty("mapx") String longitude,
        @JsonProperty("mapy") String latitude,
        @JsonProperty("firstimage") String firstImage,
        @JsonProperty("firstimage2") String thumbnailImage,
        String tel
) {
}
