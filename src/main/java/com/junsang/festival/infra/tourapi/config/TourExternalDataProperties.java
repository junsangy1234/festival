package com.junsang.festival.infra.tourapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festival.external-data")
public record TourExternalDataProperties(
        String relatedBaseYearMonth,
        int visitorReferenceYearsBack
) {
}
