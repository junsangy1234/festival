package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourFestivalDetailInfoItem(
        String serialnum,
        String infoname,
        String infotext,
        @JsonProperty("contentid") String contentId
) {
}
