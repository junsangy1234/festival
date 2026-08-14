package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourFestivalImageApiResponse(TourApiEnvelope<TourFestivalImageItem> response) {
}
