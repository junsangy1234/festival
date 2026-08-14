package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourFestivalImageItem(
        String serialnum,
        @JsonProperty("originimgurl") String imageUrl,
        @JsonProperty("thumbnailimgurl") String thumbnailUrl,
        String imgname
) {
}
