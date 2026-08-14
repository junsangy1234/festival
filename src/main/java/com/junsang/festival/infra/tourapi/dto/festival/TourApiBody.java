package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiBody<T>(
        TourApiItems<T> items,
        @JsonProperty("numOfRows") Integer numOfRows,
        @JsonProperty("pageNo") Integer pageNo,
        @JsonProperty("totalCount") Integer totalCount
) {
}
