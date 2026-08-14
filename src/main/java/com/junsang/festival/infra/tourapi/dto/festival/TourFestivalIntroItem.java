package com.junsang.festival.infra.tourapi.dto.festival;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourFestivalIntroItem(
        @JsonProperty("eventstartdate") String eventStartDate,
        @JsonProperty("eventenddate") String eventEndDate,
        @JsonProperty("eventplace") String eventPlace,
        String playtime,
        String program,
        String sponsor1,
        String sponsor1tel,
        String sponsor2,
        String sponsor2tel,
        String eventhomepage,
        String usetimefestival,
        String spendtimefestival
) {
}
